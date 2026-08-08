package com.closr.domain.garment.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.garment.dto.ResponseGarmentDto;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.garment.service.GarmentService;
import com.closr.domain.user.service.SessionService;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 의류 목록 컨트롤러 테스트.
 *
 * <p>프론트가 이 응답으로 피팅룸 목록을 그리므로 구조를 고정합니다.
 */
@WebMvcTest(GarmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class GarmentControllerTest {

    @MockBean
    private GarmentService garmentService;

    @MockBean
    private SessionService sessionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("의류 목록을 봉투에 담아 반환한다")
    void returnsGarmentList() throws Exception {
        given(garmentService.getGarmentList()).willReturn(new ResponseGarmentListDto(List.of(
                new ResponseGarmentDto(1L, "베이직 티셔츠", null, "top", List.of("S", "M", "L")),
                new ResponseGarmentDto(2L, "슬랙스", null, "bottom", List.of())
        )));

        mockMvc.perform(get("/api/v1/garments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.garments", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.garments[0].garmentId").value(1))
                .andExpect(jsonPath("$.data.garments[0].name").value("베이직 티셔츠"))
                .andExpect(jsonPath("$.data.garments[0].category").value("top"))
                .andExpect(jsonPath("$.data.garments[0].sizes[0]").value("S"))
                .andExpect(jsonPath("$.data.garments[0].sizes[2]").value("L"));
    }

    @Test
    @DisplayName("등록된 의류가 없어도 빈 목록을 반환한다")
    void returnsEmptyListWhenNoGarments() throws Exception {
        given(garmentService.getGarmentList()).willReturn(new ResponseGarmentListDto(List.of()));

        mockMvc.perform(get("/api/v1/garments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.garments", Matchers.hasSize(0)));
    }
}
