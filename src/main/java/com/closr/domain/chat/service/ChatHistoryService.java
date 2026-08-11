package com.closr.domain.chat.service;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.chat.dto.ResponseChatHistoryDto;
import com.closr.domain.chat.dto.ResponseChatMessageDto;
import com.closr.domain.chat.entity.ChatMessage;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.ChatRole;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.chat.repository.ChatMessageRepository;
import com.closr.domain.chat.repository.ConversationRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대화 기록 저장 · 조회.
 *
 * <p>챗봇 응답 스트리밍은 이 클래스가 다루지 않습니다. 여기는 기록만 담당하고,
 * AI 서버 호출은 챗봇 중계가 맡습니다. 스트리밍 도중 예외가 나도 이미 저장된
 * 사용자 발화는 남아야 하므로 저장 시점을 분리했습니다.
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    /**
     * AI 서버로 보낼 최근 턴 수.
     *
     * <p>대회 기간에는 사실상 무제한입니다. 음성 발화 200턴이면 사용자 발화가
     * 100번이라, 대화가 이 값을 넘겨 앞부분을 잊는 일은 일어나지 않습니다.
     *
     * <p><b>그래도 상한을 남기는 이유는 비용이 아니라 실패입니다.</b> LLM 은
     * 무상태라 매 턴 이전 대화를 전부 다시 보내야 하는데, 상한이 없으면 결국
     * 모델 컨텍스트 한계를 넘어 요청 자체가 실패합니다. 느려지는 것이 아니라
     * 답을 못 합니다. 턴당 약 100토큰이라 한계에 닿는 지점은 1,000턴쯤이고
     * 정상 사용으로는 가지 않지만, 개발 중 재시도 루프나 실수로 한 대화를 계속
     * 이어가면 닿습니다. 상수 하나라 무제한과 코드 복잡도가 같아, 안전망을
     * 남겨두는 편이 이득입니다.
     *
     * <p>이 값을 넘기는 대화가 실제로 생기면 밀려나는 대화를 요약해 앞에 붙이는
     * 방식을 검토합니다. 요약에 LLM 호출이 한 번 더 들어가므로 필요해진 뒤에
     * 넣습니다.
     */
    private static final int HISTORY_LIMIT = 200;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 대화를 새로 엽니다.
     *
     * <p>avatar 는 fitting 모드에서만 넘깁니다. onboarding 은 아바타 이전
     * 단계라 null 입니다.
     */
    @Transactional
    public Conversation open(Session session, ChatMode mode, Avatar avatar) {
        Conversation conversation = Conversation.builder()
                .conversationId(newId("cv_"))
                .session(session)
                .mode(mode)
                .avatar(avatar)
                .build();
        return conversationRepository.save(conversation);
    }

    /** 발화 한 건을 남깁니다. 사용자 발화와 챗봇 답변 모두 이 메서드를 씁니다. */
    @Transactional
    public ChatMessage append(Conversation conversation, ChatRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .messageId(newId("msg_"))
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        return chatMessageRepository.save(message);
    }

    /**
     * 세션이 소유한 대화를 찾습니다.
     *
     * <p>남의 대화면 403 이 아니라 404 를 냅니다. 403 은 "그 대화가 있다" 는
     * 사실을 알려주므로, 존재 여부까지 감춥니다.
     */
    @Transactional(readOnly = true)
    public Conversation findOwned(Session session, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        if (!conversation.isOwnedBy(session)) {
            throw new CustomException(ErrorCode.CHAT_NOT_FOUND);
        }
        return conversation;
    }

    /** 화면 복원용 전체 기록. 오래된 순입니다. */
    @Transactional(readOnly = true)
    public ResponseChatHistoryDto findHistory(Session session, String conversationId) {
        Conversation conversation = findOwned(session, conversationId);
        List<ResponseChatMessageDto> messages =
                chatMessageRepository.findAllByConversationOrderByIdAsc(conversation).stream()
                        .map(ResponseChatMessageDto::from)
                        .toList();
        return new ResponseChatHistoryDto(
                conversation.getConversationId(),
                conversation.getMode(),
                conversation.getAvatar() == null ? null : conversation.getAvatar().getId(),
                messages
        );
    }

    /**
     * AI 서버로 보낼 최근 히스토리. 오래된 순으로 돌려줍니다.
     *
     * <p>LLM 은 메시지 순서를 문맥으로 읽으므로 뒤집어 보내면 대화가 거꾸로
     * 이해됩니다. 최신순으로 잘라낸 뒤 여기서 되돌립니다.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> recentHistory(Conversation conversation) {
        List<ChatMessage> recent = chatMessageRepository.findRecent(
                conversation, PageRequest.of(0, HISTORY_LIMIT));
        List<ChatMessage> ordered = new ArrayList<>(recent);
        Collections.reverse(ordered);
        return ordered;
    }

    /**
     * 공개용 식별자를 만듭니다.
     *
     * <p>DB 기본키를 노출하면 순번을 바꿔 남의 대화를 찾아볼 수 있습니다.
     * 아바타의 jobId 와 같은 방식입니다.
     */
    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
