package com.closr.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(get("/api/v1/avatars/1/garments/{garmentId}/fit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value("garmentId"));
    }

    @Test
    @DisplayName("허용되지 않은 메서드는 405 를 반환한다")
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/sessions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }
}
