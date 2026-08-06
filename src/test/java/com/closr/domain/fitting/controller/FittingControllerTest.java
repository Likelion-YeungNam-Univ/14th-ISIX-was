package com.closr.domain.fitting.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.user.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;

/**
 * 피팅 목 컨트롤러 응답 계약 테스트.
 *
 * <p>목 컨트롤러라 값 자체보다 <b>응답 구조</b>를 고정하는 것이 목적입니다.
 * 프론트가 이 형태에 맞춰 화면을 붙이므로, 명세가 바뀌면 여기서 먼저 깨져야 합니다.
 */
@WebMvcTest(FittingController.class)
@AutoConfigureMockMvc(addFilters = false)
class FittingControllerTest {

    @MockBean
    private SessionService sessionService;

    private static final String PATH = "/api/v1/avatars/{avatarId}/garments/{garmentId}/fit";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("요청한 garmentId 를 그대로 돌려준다")
    void returnsRequestedGarmentId() throws Exception {
        mockMvc.perform(get(PATH, 1, 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.garmentId").value(42));
    }

    @Test
    @DisplayName("S · M · L 세 사이즈를 모두 반환하고 M 만 추천으로 표시한다")
    void returnsThreeSizesWithOnlyMediumRecommended() throws Exception {
        mockMvc.perform(get(PATH, 1, 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedSize").value("M"))
                .andExpect(jsonPath("$.data.sizes.S.recommended").value(false))
                .andExpect(jsonPath("$.data.sizes.M.recommended").value(true))
                .andExpect(jsonPath("$.data.sizes.L.recommended").value(false));
    }

    @Test
    @DisplayName("사이즈마다 부위 5개의 여유량과 판정을 반환한다")
    void returnsFivePartsPerSize() throws Exception {
        mockMvc.perform(get(PATH, 1, 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sizes.M.parts", Matchers.hasSize(5)))
                .andExpect(jsonPath("$.data.sizes.M.parts[0].part").value("shoulder_width"))
                .andExpect(jsonPath("$.data.sizes.M.parts[0].ease").value(1.2))
                .andExpect(jsonPath("$.data.sizes.M.parts[0].verdict").value("good"));
    }

    @Test
    @DisplayName("여유량이 음수인 부위는 tight, 기준을 넘으면 loose 로 판정한다")
    void classifiesEaseIntoVerdict() throws Exception {
        mockMvc.perform(get(PATH, 1, 42))
                .andExpect(status().isOk())
                // S 의 어깨는 -0.8 이라 조입니다.
                .andExpect(jsonPath("$.data.sizes.S.parts[0].verdict").value("tight"))
                // L 의 가슴은 10.8 이라 남습니다.
                .andExpect(jsonPath("$.data.sizes.L.parts[1].verdict").value("loose"));
    }
}
