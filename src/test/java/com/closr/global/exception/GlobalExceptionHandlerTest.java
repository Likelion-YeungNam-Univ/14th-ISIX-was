package com.closr.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.user.entity.Session;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 전역 예외 처리 테스트.
 *
 * <p>정적 리소스 탐색까지 포함한 실제 매핑 동작을 봐야 하므로 슬라이스가 아닌
 * 전체 컨텍스트로 띄웁니다.
 *
 * <p>Spring Boot 3.2 부터 핸들러가 없는 요청은 NoResourceFoundException 으로
 * 올라오는데, 이를 catch-all 이 잡아 모든 404 가 500 으로 나가던 회귀를 막습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 챗 컨트롤러가 세션을 요구합니다. 본문 파싱 전에 터지므로 값은 쓰이지 않습니다. */
    private Session session() {
        return Session.builder()
                .sessionToken("test-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("존재하지 않는 경로는 500 이 아니라 404 를 반환한다")
    void unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("경로변수 타입이 맞지 않으면 400 과 문제 필드를 반환한다")
    void pathVariableTypeMismatchReturnsBadRequest() throws Exception {
        // 피팅 경로는 세션을 요구합니다. MockMvc 는 /api/v1/* 로 등록된 인증 필터를
        // 태우지 않으므로, 세션 속성을 직접 넣어야 컨트롤러까지 도달합니다.
        mockMvc.perform(get("/api/v1/avatars/1/garments/{garmentId}/fit", "abc")
                        .requestAttr("session", Session.builder()
                                .sessionToken("test-token")
                                .expiresAt(LocalDateTime.now().plusDays(1))
                                .build()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value("garmentId"));
    }


    @Test
    @DisplayName("오류 응답의 Content-Type 에 charset=UTF-8 을 명시한다")
    void errorResponseDeclaresUtf8Charset() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
    }

    @Test
    @DisplayName("한글 오류 메시지가 깨지지 않는다")
    void koreanErrorMessageIsNotGarbled() throws Exception {
        String body = mockMvc.perform(get("/api/v1/does-not-exist"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("요청하신 경로를 찾을 수 없습니다");
        // Latin-1 로 잘못 해석했을 때 나타나는 형태가 섞여 있으면 안 됩니다.
        assertThat(body).doesNotContain("ì");
    }

    @Test
    @DisplayName("본문 타입이 맞지 않으면 500 이 아니라 400 을 반환한다")
    void wrongBodyTypeReturnsBadRequest() throws Exception {
        // garmentId 는 숫자인데 문자열을 보낸 경우입니다. 명세 예시가 예전에
        // "shirt_slim" 이었어서 실제로 이렇게 보내는 클라이언트가 있었습니다.
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("session", session())
                        .content("{\"mode\":\"onboarding\",\"garmentId\":\"shirt_slim\",\"message\":\"안녕\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("JSON 문법이 깨져도 500 이 아니라 400 을 반환한다")
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("session", session())
                        .content("{\"mode\":\"onboarding\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("본문 오류 응답에 내부 클래스 이름을 노출하지 않는다")
    void doesNotLeakInternalNames() throws Exception {
        // Jackson 메시지에는 패키지 경로가 들어 있어 그대로 내보내면 구조가 드러납니다.
        String body = mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("session", session())
                        .content("{\"mode\":\"onboarding\",\"garmentId\":\"x\",\"message\":\"안녕\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("com.closr");
        assertThat(body).doesNotContain("RequestChatDto");
    }

    @Test
    @DisplayName("허용되지 않은 메서드는 405 를 반환한다")
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/sessions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }
}
