package com.closr.domain.fitting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.fitting.dto.ResponseSizeOptionsDto;
import com.closr.domain.fitting.service.FittingService;
import com.closr.domain.user.entity.Session;
import com.closr.domain.user.service.SessionService;
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
 * 피팅 컨트롤러 응답 계약 테스트.
 *
 * <p>프론트가 이 형태로 화면을 그리므로 구조를 고정합니다.
 * 판정 계산 자체는 {@code FittingServiceTest} 에서 확인합니다.
 */
@WebMvcTest(FittingController.class)
@AutoConfigureMockMvc(addFilters = false)
class FittingControllerTest {

    private static final String PATH = "/api/v1/avatars/{avatarId}/garments/{garmentId}/fit";

    @MockBean
    private FittingService fittingService;

    @MockBean
    private SessionService sessionService;

    @Autowired
    private MockMvc mockMvc;

    private Session session;

    @BeforeEach
    void setUp() {
        session = Mockito.mock(Session.class);
    }

    private ResponseSizeDetailDto size(boolean recommended, boolean wearable, String verdict) {
        return new ResponseSizeDetailDto(
                null,
                List.of(new ResponseFitPartDto("chest_circ", 11.0, 14.0, -3.0, verdict, "green")),
                recommended,
                wearable);
    }

    @Test
    @DisplayName("사이즈별 판정과 추천 사이즈를 반환한다")
    void returnsFittingResult() throws Exception {
        given(fittingService.getFitting(any(), eq(1L), eq(2L))).willReturn(new ResponseFittingDto(
                2L,
                new ResponseSizeOptionsDto(
                        size(true, true, "적정"),
                        size(false, true, "여유 있음"),
                        size(false, false, "꽉 낌")),
                "S",
                "가슴둘레 88.0cm 기준 S 사이즈가 모든 부위에서 적정합니다."));

        mockMvc.perform(get(PATH, 1, 2).requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.garmentId").value(2))
                .andExpect(jsonPath("$.data.recommendedSize").value("S"))
                .andExpect(jsonPath("$.data.sizes.S.recommended").value(true))
                .andExpect(jsonPath("$.data.sizes.S.wearable").value(true))
                .andExpect(jsonPath("$.data.sizes.L.wearable").value(false));
    }

    @Test
    @DisplayName("부위별로 여유량 · 목표 여유 · 편차 · 판정을 함께 반환한다")
    void returnsDeviationDetailsPerPart() throws Exception {
        given(fittingService.getFitting(any(), eq(1L), eq(2L))).willReturn(new ResponseFittingDto(
                2L,
                new ResponseSizeOptionsDto(size(true, true, "적정"), null, null),
                "S",
                "사유"));

        mockMvc.perform(get(PATH, 1, 2).requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sizes.S.parts", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].part").value("chest_circ"))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].ease").value(11.0))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].refEase").value(14.0))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].deviation").value(-3.0))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].verdict").value("적정"))
                .andExpect(jsonPath("$.data.sizes.S.parts[0].color").value("green"));
    }
}
