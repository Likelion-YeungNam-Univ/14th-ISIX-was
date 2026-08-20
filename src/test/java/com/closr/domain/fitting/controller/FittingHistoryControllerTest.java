package com.closr.domain.fitting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.fitting.dto.ResponseFittingRecordDetailDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.fitting.service.FittingRecordService;
import com.closr.domain.user.entity.Session;
import com.closr.domain.user.service.SessionService;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * 피팅 기록 조회 테스트.
 *
 * <p>필터는 addFilters=false 로 꺼두고 세션은 request attribute 로 직접 넣습니다.
 */
@WebMvcTest(FittingHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class FittingHistoryControllerTest {

    @MockBean
    private FittingRecordService fittingRecordService;

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
    @DisplayName("피팅 기록을 최신순 목록으로 반환한다")
    void returnsMyFittings() throws Exception {
        given(fittingRecordService.findMine(any())).willReturn(new ResponseFittingRecordListDto(List.of(
                new ResponseFittingRecordDto(2L, 1L, 3L, "오버핏 셔츠", "M", true, LocalDateTime.now()),
                new ResponseFittingRecordDto(1L, 1L, 1L, "베이직 티셔츠", "S", true, LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/v1/fittings/me").requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fittings", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.fittings[0].fittingId").value(2))
                .andExpect(jsonPath("$.data.fittings[0].garmentName").value("오버핏 셔츠"));
    }

    @Test
    @DisplayName("기록이 없으면 빈 목록을 반환한다")
    void returnsEmptyList() throws Exception {
        given(fittingRecordService.findMine(any()))
                .willReturn(new ResponseFittingRecordListDto(List.of()));

        mockMvc.perform(get("/api/v1/fittings/me").requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fittings", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("저장된 피팅 결과를 그대로 반환한다")
    void returnsStoredResult() throws Exception {
        given(fittingRecordService.findOne(any(), eq(7L))).willReturn(
                new ResponseFittingRecordDetailDto(7L, 1L, 1L, "베이직 티셔츠", LocalDateTime.now(),
                        Map.of("recommendedSize", "S", "sizes", Map.of("S", Map.of("recommended", true)))));

        mockMvc.perform(get("/api/v1/fittings/{id}", 7).requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fittingId").value(7))
                .andExpect(jsonPath("$.data.result.recommendedSize").value("S"))
                .andExpect(jsonPath("$.data.result.sizes.S.recommended").value(true));
    }

    @Test
    @DisplayName("다른 세션의 기록은 403 을 반환한다")
    void rejectsOtherSessionsRecord() throws Exception {
        willThrow(new CustomException(ErrorCode.FORBIDDEN))
                .given(fittingRecordService).findOne(any(), eq(99L));

        mockMvc.perform(get("/api/v1/fittings/{id}", 99).requestAttr("session", session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("기록을 지우면 본문 없이 204 를 반환한다")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/fittings/{fittingId}", 7L)
                        .requestAttr("session", session))
                .andExpect(status().isNoContent());

        Mockito.verify(fittingRecordService).delete(any(), eq(7L));
    }

    @Test
    @DisplayName("없는 기록이거나 남의 기록이면 404 를 반환한다")
    void deleteReturnsNotFoundForOthers() throws Exception {
        // 403 은 "그 기록이 있다" 를 알려줍니다. 존재 여부까지 감춥니다.
        willThrow(new CustomException(ErrorCode.FITTING_NOT_FOUND))
                .given(fittingRecordService).delete(any(), eq(999L));

        mockMvc.perform(delete("/api/v1/fittings/{fittingId}", 999L)
                        .requestAttr("session", session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FITTING_NOT_FOUND"));
    }
}
