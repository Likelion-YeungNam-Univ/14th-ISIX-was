package com.closr.domain.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.chat.dto.ResponseChatHistoryDto;
import com.closr.domain.chat.dto.ResponseChatMessageDto;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.ChatRole;
import com.closr.domain.chat.service.ChatHistoryService;
import com.closr.domain.user.entity.Session;
import com.closr.domain.user.service.SessionService;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 대화 기록 조회 테스트.
 *
 * <p>필터는 addFilters=false 로 꺼두고 세션은 request attribute 로 직접 넣습니다.
 *
 * <p>role 이 소문자로 나가는지 확인합니다. 엔티티는 USER · ASSISTANT 로 저장하는데
 * LLM API 와 명세는 소문자를 쓰므로, 직렬화가 어긋나면 프론트가 발화자를 구분하지
 * 못하고 AI 서버도 히스토리를 못 읽습니다.
 */
@WebMvcTest(ChatHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatHistoryControllerTest {

    @MockBean
    private ChatHistoryService chatHistoryService;

    @MockBean
    private SessionService sessionService;

    @Autowired
    private MockMvc mockMvc;

    private Session session;

    @BeforeEach
    void setUp() {
        session = Mockito.mock(Session.class);
    }

    @Test
    @DisplayName("대화 기록을 오래된 순으로 반환한다")
    void returnsHistory() throws Exception {
        given(chatHistoryService.findHistory(any(), any())).willReturn(
                new ResponseChatHistoryDto("cv_9f21ab7c4d20", ChatMode.FITTING, 3L, List.of(
                        new ResponseChatMessageDto("msg_a1b2c3d4e5f6", ChatRole.USER,
                                "이 셔츠 S 저한테 맞을까요?", LocalDateTime.now()),
                        new ResponseChatMessageDto("msg_b2c3d4e5f6a1", ChatRole.ASSISTANT,
                                "어깨가 기준보다 2.6cm 부족합니다. M을 권합니다.", LocalDateTime.now())
                )));

        mockMvc.perform(get("/api/v1/chat/{conversationId}", "cv_9f21ab7c4d20")
                        .requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversationId").value("cv_9f21ab7c4d20"))
                // mode · role 은 소문자여야 합니다. 대문자로 나가면 AI 서버가 못 읽습니다.
                .andExpect(jsonPath("$.data.mode").value("fitting"))
                .andExpect(jsonPath("$.data.avatarId").value(3))
                .andExpect(jsonPath("$.data.messages", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.data.messages[0].messageId").value("msg_a1b2c3d4e5f6"));
    }

    @Test
    @DisplayName("onboarding 대화는 avatarId 가 null 이다")
    void returnsNullAvatarIdForOnboarding() throws Exception {
        given(chatHistoryService.findHistory(any(), any())).willReturn(
                new ResponseChatHistoryDto("cv_0011223344aa", ChatMode.ONBOARDING, null, List.of()));

        mockMvc.perform(get("/api/v1/chat/{conversationId}", "cv_0011223344aa")
                        .requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("onboarding"))
                .andExpect(jsonPath("$.data.avatarId").doesNotExist())
                .andExpect(jsonPath("$.data.messages", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("없는 대화이거나 다른 세션의 대화면 404 를 반환한다")
    void returnsNotFoundForOthers() throws Exception {
        willThrow(new CustomException(ErrorCode.CHAT_NOT_FOUND))
                .given(chatHistoryService).findHistory(any(), any());

        mockMvc.perform(get("/api/v1/chat/{conversationId}", "cv_someoneelse00")
                        .requestAttr("session", session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CHAT_NOT_FOUND"));
    }
}
