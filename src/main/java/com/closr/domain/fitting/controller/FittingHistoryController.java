package com.closr.domain.fitting.controller;

import com.closr.api.FittingHistoryApi;
import com.closr.domain.fitting.dto.ResponseFittingRecordDetailDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.fitting.service.FittingRecordService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피팅 기록 조회 컨트롤러.
 *
 * <p>매핑과 스웨거 명세는 {@link FittingHistoryApi} 에 있습니다.
 */
@RestController
@RequiredArgsConstructor
public class FittingHistoryController implements FittingHistoryApi {

    private final FittingRecordService fittingRecordService;

    @Override
    public ResponseEntity<ApiResponse<ResponseFittingRecordListDto>> getMyFittings(
            @RequestAttribute("session") Session session) {
        return ResponseEntity.ok(ApiResponse.ok(fittingRecordService.findMine(session)));
    }

    @Override
    public ResponseEntity<ApiResponse<ResponseFittingRecordDetailDto>> getFitting(
            @RequestAttribute("session") Session session, Long fittingId) {
        return ResponseEntity.ok(ApiResponse.ok(fittingRecordService.findOne(session, fittingId)));
    }
}
