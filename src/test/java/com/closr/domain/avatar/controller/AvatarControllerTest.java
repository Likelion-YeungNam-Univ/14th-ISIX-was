package com.closr.domain.avatar.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 아바타 목 컨트롤러 테스트.
 *
 * <p>응답 형태와 함께 키 · 몸무게 범위 검증이 실제로 동작하는지 확인합니다.
 */
@WebMvcTest(AvatarController.class)
class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "body.jpg", "image/jpeg", "fake-image".getBytes());
    }

    @Test
    @DisplayName("아바타 생성 요청은 202 와 jobId 를 반환한다")
    void createAvatarReturnsAccepted() throws Exception {
        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", "165")
                        .param("weight", "55"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").isNotEmpty());
    }

    @ParameterizedTest(name = "height={0}, weight={1} 이면 {2} 필드가 거부된다")
    @CsvSource({
            "129, 55, height",
            "201, 55, height",
            "165, 29, weight",
            "165, 151, weight"
    })
    @DisplayName("범위를 벗어난 키 · 몸무게는 400 과 함께 문제 필드를 알려준다")
    void rejectsOutOfRangeMeasurements(String height, String weight, String field) throws Exception {
        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", height)
                        .param("weight", weight))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value(field));
    }

    @ParameterizedTest(name = "height={0}, weight={1} 은 경계값이라 통과한다")
    @CsvSource({"130, 30", "200, 150"})
    @DisplayName("경계값은 허용한다")
    void acceptsBoundaryMeasurements(String height, String weight) throws Exception {
        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", height)
                        .param("weight", weight))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("상태 조회는 12부위 치수를 반환한다")
    void getAvatarStatusReturnsMeasurements() throws Exception {
        mockMvc.perform(get("/api/v1/avatars/{jobId}", "mock-job-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"))
                .andExpect(jsonPath("$.data.measurements.chest_circ").value(87.2));
    }
}
