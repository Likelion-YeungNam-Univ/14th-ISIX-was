package com.closr.domain.chat.service;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.chat.dto.RequestChatDto;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.ChatRole;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import com.closr.infra.ai.AiChatClient;
import com.closr.infra.ai.AiChatRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 챗봇 중계.
 *
 * <p>프론트 요청을 AI 서버로 넘기고, 오는 SSE 를 그대로 흘려보냅니다.
 *
 * <pre>
 * [브라우저] ──SSE── [여기] ──SSE── [AI 서버]
 * </pre>
 *
 * <p><b>검증은 스트림을 열기 전에 끝냅니다.</b> 응답 헤더가 나간 뒤에는 상태
 * 코드를 바꿀 수 없어, 늦게 검사하면 프론트가 같은 실패를 HTTP 와 SSE 두 곳에서
 * 처리해야 합니다. 그래서 대화 소유권·아바타 확인을 모두 마친 뒤에
 * {@link StreamingResponseBody} 를 만듭니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    /** 음성 발화 한 번 분량입니다. AI 서버의 MAX_MESSAGE_LEN 과 같은 값입니다. */
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final ChatRateLimiter chatRateLimiter;
    private final ChatHistoryService chatHistoryService;
    private final AvatarRepository avatarRepository;
    private final FitContextAssembler fitContextAssembler;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;

    /**
     * 대화 한 턴을 중계합니다.
     *
     * <p>돌려주는 {@link StreamingResponseBody} 는 요청 스레드 밖에서 실행됩니다.
     * 그래서 DB 에서 읽어야 하는 값은 여기서 미리 꺼내 둡니다. {@code
     * open-in-view} 가 꺼져 있어 스트림 안에서 지연 로딩을 하면 실패합니다.
     */
    public StreamingResponseBody relay(Session session, RequestChatDto request) {
        // 길이를 먼저 봅니다. 500자를 넘긴 요청이 한도를 깎을 이유가 없습니다.
        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_TOO_LONG);
        }

        // 그다음 한도입니다. 뒤에 두면 한도를 넘긴 요청도 DB 조회를 다 하고 나서
        // 거절됩니다.
        chatRateLimiter.check(session.getId());

        Avatar avatar = resolveAvatar(session, request);
        Conversation conversation = resolveConversation(session, request, avatar);

        chatHistoryService.append(conversation, ChatRole.USER, request.message());

        List<AiChatRequest.Turn> history = chatHistoryService.recentHistory(conversation).stream()
                .map(message -> new AiChatRequest.Turn(
                        message.getRole().getValue(), message.getContent()))
                .toList();

        // fit_context 도 여기서 만듭니다. 스트림 안에서 DB 를 읽으면
        // open-in-view 가 꺼져 있어 지연 로딩이 실패합니다.
        // 지난 대화 요약을 되돌려 보냅니다. 첫 대화면 null 이고, 그때는 AI 가
        // 아는 척하지 않습니다.
        Map<String, Object> fitContext = avatar == null ? null
                : fitContextAssembler.assemble(session, avatar, request.garmentId(),
                        request.size(), conversation.getSummary());

        AiChatRequest aiRequest = new AiChatRequest(
                request.mode().getValue(), request.message(), history, fitContext);

        String conversationId = conversation.getConversationId();
        Long conversationPk = conversation.getId();
        return out -> stream(out, conversationId, conversationPk, conversation, aiRequest);
    }

    /**
     * mode 가 fitting 이면 아바타가 있어야 합니다.
     *
     * <p>onboarding 은 아바타 이전 화면이라 없는 것이 정상입니다. 그 상태에서
     * 치수를 묻는 질문이 오면 AI 가 "먼저 아바타를 만들어주세요" 로 안내합니다.
     */
    private Avatar resolveAvatar(Session session, RequestChatDto request) {
        if (request.mode() != ChatMode.FITTING) {
            return null;
        }
        if (request.avatarId() == null) {
            throw new CustomException(ErrorCode.CHAT_AVATAR_REQUIRED);
        }

        Avatar avatar = avatarRepository.findById(request.avatarId())
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));
        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return avatar;
    }

    /**
     * 이어받을 대화를 찾거나 새로 엽니다.
     *
     * <p>{@code conversationId} 를 주면 그 대화를 이어씁니다. 홈에서 시작한 대화를
     * 피팅룸에서 이어받는 경로가 여기입니다 — 화면이 바뀌어도 앞서 한 말이 유지됩니다.
     *
     * <p><b>이어받은 대화에 아바타를 붙입니다.</b> 홈 대화는 아바타 없이 열리는데,
     * 그대로 두면 상담 요약이 비어서 나갑니다. 요약은 대화에 매달린 아바타로 피팅
     * 기록을 찾기 때문입니다. 조용히 빈 값이 되는 종류라 여기서 막습니다.
     */
    private Conversation resolveConversation(Session session, RequestChatDto request, Avatar avatar) {
        if (request.conversationId() == null || request.conversationId().isBlank()) {
            return chatHistoryService.open(session, request.mode(), avatar);
        }

        Conversation conversation =
                chatHistoryService.findOwned(session, request.conversationId());
        if (avatar != null) {
            chatHistoryService.linkAvatar(conversation.getId(), avatar);
        }
        return conversation;
    }

    private void stream(OutputStream out, String conversationId, Long conversationPk,
                        Conversation conversation, AiChatRequest aiRequest) {
        StringBuilder answer = new StringBuilder();
        Map<String, Object> summary = new LinkedHashMap<>();

        try {
            // 첫 이벤트로 대화 식별자를 알립니다. 신규 대화면 프론트가 이 값을
            // 다음 요청에 실어 보냅니다.
            write(out, Map.of("conversationId", conversationId));

            aiChatClient.stream(aiRequest, line -> relayLine(out, line, answer, summary));
        } catch (ClientGoneException e) {
            // 사용자가 화면을 닫았습니다. 여기서 멈추면 남은 토큰을 받지 않아
            // 과금이 줄어듭니다. 서버 오류가 아니라 debug 로 남깁니다.
            log.debug("클라이언트가 연결을 끊어 중계를 멈춥니다");
        } catch (CustomException e) {
            // 이미 conversationId 를 보냈으므로 상태 코드를 바꿀 수 없습니다.
            log.error("챗 중계가 중단되었습니다: {}", e.getErrorCode());
            writeErrorQuietly(out, ErrorCode.CHAT_UPSTREAM_ERROR);
        } finally {
            // 중간에 끊겼어도 받은 만큼은 남깁니다. 화면에 보인 답변이 기록에
            // 없으면 새로고침했을 때 대화가 어긋납니다.
            if (!answer.isEmpty()) {
                chatHistoryService.append(conversation, ChatRole.ASSISTANT, answer.toString());
            }
            // 요약이 없어도 대화는 정상입니다. 다음 턴에 다시 뽑습니다.
            if (!summary.isEmpty()) {
                chatHistoryService.updateSummary(conversationPk, summary);
            }
        }
    }

    /**
     * AI 가 보낸 한 줄을 프론트로 넘깁니다.
     *
     * <p>두 가지를 합니다. {@code delta} 를 모아 답변 전문을 만들고,
     * {@code done} 에 붙은 {@code summary} 를 떼어냅니다.
     *
     * <p><b>{@code summary} 는 백엔드만 소비합니다.</b> 대화 요약을 저장하는
     * 용도라 프론트가 쓸 값이 아니고, 그대로 흘리면 프론트가 모르는 필드를
     * 받습니다. 저장 로직은 별건이라 지금은 떼어내고 로그만 남깁니다.
     */
    private void relayLine(OutputStream out, String line, StringBuilder answer,
                           Map<String, Object> summary) {
        if (!line.startsWith("data: ")) {
            return;
        }

        String json = line.substring("data: ".length());
        JsonNode node = read(json);
        if (node == null) {
            // 형태를 모르는 줄은 버립니다. 그대로 넘기면 프론트 파서가 깨집니다.
            log.warn("AI 챗 응답을 해석할 수 없습니다: {}", json);
            return;
        }

        if (node.hasNonNull("delta")) {
            answer.append(node.get("delta").asText());
        }

        if (node.has("summary")) {
            // 백엔드만 소비합니다. 프론트로 넘기면 모르는 필드를 받습니다.
            JsonNode extracted = ((ObjectNode) node).remove("summary");
            summary.putAll(objectMapper.convertValue(extracted, new TypeReference<>() {}));
        }

        write(out, node);
    }

    private JsonNode read(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject() ? node : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void write(OutputStream out, Object payload) {
        try {
            byte[] body = ("data: " + objectMapper.writeValueAsString(payload) + "\n\n")
                    .getBytes(StandardCharsets.UTF_8);
            out.write(body);
            // 프록시와 서블릿 버퍼가 응답을 모아 두면 delta 가 완료 시점에
            // 한꺼번에 도착합니다. 스트리밍이 죽는데 로컬에서는 드러나지 않습니다.
            out.flush();
        } catch (IOException e) {
            // 사용자가 화면을 닫으면 여기로 옵니다. 오류가 아닙니다.
            throw new ClientGoneException(e);
        }
    }

    /** 에러 이벤트를 보낼 때만 씁니다. 이미 끊긴 연결에 쓰는 것은 오류가 아닙니다. */
    private void writeErrorQuietly(OutputStream out, ErrorCode code) {
        try {
            write(out, Map.of("error", Map.of(
                    "code", code.name(), "message", code.getMessage())));
        } catch (ClientGoneException e) {
            log.debug("클라이언트가 이미 끊어져 error 이벤트를 보내지 못했습니다");
        }
    }

    /** 클라이언트가 먼저 끊은 경우. 서버 오류로 세지 않습니다. */
    private static class ClientGoneException extends RuntimeException {
        ClientGoneException(Throwable cause) {
            super(cause);
        }
    }
}
