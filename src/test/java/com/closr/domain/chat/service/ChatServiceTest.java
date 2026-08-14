package com.closr.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.chat.dto.RequestChatDto;
import com.closr.domain.chat.entity.ChatMessage;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.ChatRole;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import com.closr.infra.ai.AiChatClient;
import com.closr.infra.ai.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 챗 중계 테스트.
 *
 * <p>여기서 지키려는 것이 둘입니다. <b>검증이 스트림을 열기 전에 끝나는지</b>와
 * <b>{@code summary} 가 프론트로 새지 않는지</b>입니다.
 */
class ChatServiceTest {

    private ChatHistoryService chatHistoryService;
    private AvatarRepository avatarRepository;
    private AiChatClient aiChatClient;
    private FitContextAssembler fitContextAssembler;
    private ChatService chatService;

    private Session session;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        chatHistoryService = Mockito.mock(ChatHistoryService.class);
        avatarRepository = Mockito.mock(AvatarRepository.class);
        aiChatClient = Mockito.mock(AiChatClient.class);
        fitContextAssembler = Mockito.mock(FitContextAssembler.class);
        chatService = new ChatService(new ChatRateLimiter(), chatHistoryService, avatarRepository,
                fitContextAssembler, aiChatClient, new ObjectMapper());

        session = Mockito.mock(Session.class);
        given(session.getId()).willReturn(1L);

        conversation = Conversation.builder()
                .conversationId("cv_9f21ab").session(session).mode(ChatMode.ONBOARDING).build();
        given(chatHistoryService.open(any(), any(), any())).willReturn(conversation);
        given(chatHistoryService.recentHistory(any())).willReturn(List.of());
    }

    /** AI 가 흘려보낼 줄을 정해줍니다. */
    private void givenAiLines(String... lines) {
        willAnswer(invocation -> {
            Consumer<String> onLine = invocation.getArgument(1);
            for (String line : lines) {
                onLine.accept(line);
            }
            return null;
        }).given(aiChatClient).stream(any(), any());
    }

    private String run(RequestChatDto request) throws Exception {
        StreamingResponseBody body = chatService.relay(session, request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        return out.toString("UTF-8");
    }

    private RequestChatDto onboarding(String message) {
        return new RequestChatDto(ChatMode.ONBOARDING, null, null, null, null, message);
    }

    @Test
    @DisplayName("첫 이벤트로 conversationId 를 알리고 delta 를 그대로 흘린다")
    void relaysDeltaAfterConversationId() throws Exception {
        givenAiLines(
                "data: {\"delta\":\"어깨가 \"}",
                "data: {\"delta\":\"2.6cm 부족합니다.\"}",
                "data: {\"done\":true}");

        String sse = run(onboarding("이 셔츠 맞을까요?"));

        assertThat(sse.lines().filter(l -> l.startsWith("data: ")).toList())
                .containsExactly(
                        "data: {\"conversationId\":\"cv_9f21ab\"}",
                        "data: {\"delta\":\"어깨가 \"}",
                        "data: {\"delta\":\"2.6cm 부족합니다.\"}",
                        "data: {\"done\":true}");
    }

    @Test
    @DisplayName("done 에 붙은 summary 는 프론트로 넘기지 않는다")
    void stripsSummaryFromDone() throws Exception {
        // summary 는 대화 요약 저장용이라 프론트가 쓸 값이 아닙니다.
        // 그대로 흘리면 프론트가 모르는 필드를 받습니다.
        givenAiLines("data: {\"done\":true,\"summary\":{\"용도\":\"출근\"}}");

        String sse = run(onboarding("안녕하세요"));

        assertThat(sse).contains("\"done\":true");
        assertThat(sse).doesNotContain("summary");
        assertThat(sse).doesNotContain("출근");
    }

    @Test
    @DisplayName("해석할 수 없는 줄은 버리고 프론트로 넘기지 않는다")
    void dropsUnparsableLines() throws Exception {
        // 반쪽 JSON 을 그대로 넘기면 프론트 파서가 깨집니다.
        givenAiLines(
                "data: {\"delta\":\"정상\"}",
                "data: {\"delta\":\"반쪽",
                ": keep-alive",
                "data: {\"done\":true}");

        String sse = run(onboarding("안녕"));

        assertThat(sse).contains("정상");
        assertThat(sse).doesNotContain("반쪽");
        assertThat(sse).doesNotContain("keep-alive");
    }

    @Test
    @DisplayName("사용자 발화와 챗봇 답변을 모두 기록한다")
    void savesBothSides() throws Exception {
        givenAiLines("data: {\"delta\":\"어깨가 \"}", "data: {\"delta\":\"맞습니다.\"}",
                "data: {\"done\":true}");

        run(onboarding("이거 어때요?"));

        verify(chatHistoryService).append(conversation, ChatRole.USER, "이거 어때요?");
        verify(chatHistoryService).append(conversation, ChatRole.ASSISTANT, "어깨가 맞습니다.");
    }

    @Test
    @DisplayName("중간에 끊겨도 받은 답변까지는 기록한다")
    void savesPartialAnswerOnFailure() throws Exception {
        // 화면에 보인 답변이 기록에 없으면 새로고침했을 때 대화가 어긋납니다.
        willAnswer(invocation -> {
            Consumer<String> onLine = invocation.getArgument(1);
            onLine.accept("data: {\"delta\":\"어깨가 \"}");
            throw new CustomException(ErrorCode.CHAT_UPSTREAM_ERROR);
        }).given(aiChatClient).stream(any(), any());

        String sse = run(onboarding("질문"));

        assertThat(sse).contains("CHAT_UPSTREAM_ERROR");
        verify(chatHistoryService).append(conversation, ChatRole.ASSISTANT, "어깨가 ");
    }

    @Test
    @DisplayName("답변이 하나도 오지 않으면 빈 기록을 남기지 않는다")
    void doesNotSaveEmptyAnswer() throws Exception {
        willAnswer(invocation -> {
            throw new CustomException(ErrorCode.CHAT_UPSTREAM_ERROR);
        }).given(aiChatClient).stream(any(), any());

        run(onboarding("질문"));

        verify(chatHistoryService, never())
                .append(any(), eq(ChatRole.ASSISTANT), any());
    }

    @Test
    @DisplayName("fitting 인데 avatarId 가 없으면 스트림을 열기 전에 막는다")
    void rejectsFittingWithoutAvatar() {
        RequestChatDto request =
                new RequestChatDto(ChatMode.FITTING, null, null, null, null, "맞나요?");

        assertThatThrownBy(() -> chatService.relay(session, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("아바타를 먼저");

        // 스트림을 열지 않았으니 AI 를 부르지도, 발화를 남기지도 않습니다.
        verify(aiChatClient, never()).stream(any(), any());
        verify(chatHistoryService, never()).append(any(), any(), any());
    }

    @Test
    @DisplayName("한도를 넘으면 스트림을 열기 전에 막고 AI 를 부르지 않는다")
    void rejectsOverRateLimitBeforeStream() throws Exception {
        // 한도를 넘긴 요청이 DB 조회와 AI 호출까지 다 하고 거절되면 의미가 없습니다.
        for (int i = 0; i < 30; i++) {
            givenAiLines("data: {\"done\":true}");
            run(onboarding("질문 " + i));
        }
        Mockito.clearInvocations(aiChatClient, chatHistoryService);

        assertThatThrownBy(() -> chatService.relay(session, onboarding("한 번 더")))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("요청이 많습니다");

        verify(aiChatClient, never()).stream(any(), any());
        verify(chatHistoryService, never()).append(any(), any(), any());
    }

    @Test
    @DisplayName("남의 아바타로는 대화할 수 없다")
    void rejectsOtherSessionsAvatar() {
        Session other = Mockito.mock(Session.class);
        given(other.getId()).willReturn(2L);
        given(avatarRepository.findById(3L)).willReturn(Optional.of(
                Avatar.builder().session(other).status("done")
                        .measurements(Map.of("chest_circ", 88.0)).build()));

        RequestChatDto request =
                new RequestChatDto(ChatMode.FITTING, null, 3L, null, null, "맞나요?");

        assertThatThrownBy(() -> chatService.relay(session, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한");
    }

    @Test
    @DisplayName("conversationId 를 주면 새 대화를 열지 않고 이어 붙인다")
    void continuesExistingConversation() throws Exception {
        given(chatHistoryService.findOwned(session, "cv_9f21ab")).willReturn(conversation);
        givenAiLines("data: {\"done\":true}");

        run(new RequestChatDto(ChatMode.ONBOARDING, "cv_9f21ab", null, null, null, "이어서"));

        verify(chatHistoryService).findOwned(session, "cv_9f21ab");
        verify(chatHistoryService, never()).open(any(), any(), any());
    }

    @Test
    @DisplayName("onboarding 은 fit_context 를 보내지 않는다")
    void sendsNoFitContextForOnboarding() throws Exception {
        // 아바타 이전 화면이라 서버가 아는 치수가 없습니다.
        givenAiLines("data: {\"done\":true}");

        run(onboarding("어떻게 써요?"));

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).stream(captor.capture(), any());
        assertThat(captor.getValue().fit_context()).isNull();
        verify(fitContextAssembler, never()).assemble(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("fitting 은 조립한 fit_context 를 그대로 실어 보낸다")
    void sendsAssembledFitContextForFitting() throws Exception {
        Avatar avatar = Avatar.builder().session(session).status("done")
                .measurements(Map.of("chest_circ", 85.3)).build();
        given(avatarRepository.findById(3L)).willReturn(Optional.of(avatar));
        given(fitContextAssembler.assemble(any(), any(), any(), any(), any()))
                .willReturn(Map.of("garment_id", "shirt_slim", "size", "s"));
        givenAiLines("data: {\"done\":true}");

        run(new RequestChatDto(ChatMode.FITTING, null, 3L, 2L, "s", "이거 맞나요?"));

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).stream(captor.capture(), any());
        assertThat(captor.getValue().fit_context())
                .containsEntry("garment_id", "shirt_slim");
    }

    @Test
    @DisplayName("done 의 summary 를 대화에 저장한다")
    void savesSummaryFromDone() throws Exception {
        givenAiLines("data: {\"delta\":\"네\"}",
                "data: {\"done\":true,\"summary\":{\"용도\":\"출근\",\"신경쓰는부위\":[\"shoulder_width\"]}}");

        run(onboarding("출근용 셔츠 찾아요"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(chatHistoryService).updateSummary(any(), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("용도", "출근")
                .containsEntry("신경쓰는부위", List.of("shoulder_width"));
    }

    @Test
    @DisplayName("summary 가 없으면 저장하지 않는다")
    void doesNotSaveWhenSummaryAbsent() throws Exception {
        // 요약이 실패해도 대화는 정상입니다. 다음 턴에 다시 뽑습니다.
        givenAiLines("data: {\"delta\":\"네\"}", "data: {\"done\":true}");

        run(onboarding("질문"));

        verify(chatHistoryService, never()).updateSummary(any(), any());
    }

    @Test
    @DisplayName("지난 요약을 profile 로 되돌려 보낸다")
    void injectsStoredSummaryAsProfile() throws Exception {
        Map<String, Object> stored = Map.of("용도", "출근");
        Conversation withSummary = Conversation.builder()
                .conversationId("cv_9f21ab").session(session).mode(ChatMode.FITTING).build();
        withSummary.updateSummary(stored);
        given(chatHistoryService.findOwned(session, "cv_9f21ab")).willReturn(withSummary);

        Avatar avatar = Avatar.builder().session(session).status("done")
                .measurements(Map.of("chest_circ", 85.3)).build();
        given(avatarRepository.findById(3L)).willReturn(Optional.of(avatar));
        given(fitContextAssembler.assemble(any(), any(), any(), any(), any()))
                .willReturn(Map.of("profile", stored));
        givenAiLines("data: {\"done\":true}");

        run(new RequestChatDto(ChatMode.FITTING, "cv_9f21ab", 3L, 2L, "s", "이건 어때요?"));

        // 조립기에 지난 요약이 그대로 넘어가야 합니다.
        verify(fitContextAssembler).assemble(session, avatar, 2L, "s", stored);
    }

    @Test
    @DisplayName("히스토리를 소문자 role 로 AI 에 보낸다")
    void sendsHistoryWithLowercaseRole() throws Exception {
        // LLM API 가 소문자 user · assistant 를 요구합니다.
        given(chatHistoryService.recentHistory(conversation)).willReturn(List.of(
                ChatMessage.builder().messageId("msg_001").conversation(conversation)
                        .role(ChatRole.USER).content("이전 질문").build(),
                ChatMessage.builder().messageId("msg_002").conversation(conversation)
                        .role(ChatRole.ASSISTANT).content("이전 답변").build()));
        givenAiLines("data: {\"done\":true}");

        run(onboarding("지금 질문"));

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).stream(captor.capture(), any());
        AiChatRequest sent = captor.getValue();

        assertThat(sent.mode()).isEqualTo("onboarding");
        assertThat(sent.history()).extracting(AiChatRequest.Turn::role)
                .containsExactly("user", "assistant");
        assertThat(sent.message()).isEqualTo("지금 질문");
    }
}
