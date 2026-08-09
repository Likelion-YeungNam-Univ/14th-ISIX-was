package com.closr.api;

import com.closr.domain.fitting.dto.ResponseFittingRecordDetailDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "FittingHistory", description = "피팅 기록 조회 API")
@RequestMapping("/api/v1/fittings")
public interface FittingHistoryApi {

    @Operation(summary = "내 피팅 기록 목록",
            description = "세션에 저장된 피팅 기록을 최신순으로 반환합니다. 상세는 fittingId 로 다시 조회합니다.")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<ResponseFittingRecordListDto>> getMyFittings(
            @Parameter(hidden = true) @RequestAttribute("session") Session session
    );

    @Operation(summary = "저장된 피팅 결과 조회",
            description = "피팅 당시의 결과를 그대로 반환합니다. 다시 계산하지 않으므로 판정 기준이 바뀌어도 그때 본 값이 나옵니다.")
    @GetMapping("/{fittingId}")
    ResponseEntity<ApiResponse<ResponseFittingRecordDetailDto>> getFitting(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "피팅 기록 ID") @PathVariable("fittingId") Long fittingId
    );
}
