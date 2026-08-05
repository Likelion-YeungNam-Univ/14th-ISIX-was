package com.closr.api;

import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Fitting", description = "가상 피팅 및 사이즈 추천 API")
@RequestMapping("/api/v1/fittings")
public interface FittingApi {

    @Operation(summary = "사이즈 추천 및 여유량 조회", description = "사용자의 실측 치수를 기준으로 선택한 의류의 사이즈별 여유량과 추천 사이즈를 반환합니다.")
    @GetMapping("/{garmentId}")
    ResponseEntity<ApiResponse<ResponseFittingDto>> getFittingRecommendation(
            @Parameter(description = "선택한 의류의 고유 ID")
            @PathVariable("garmentId") Long garmentId
    );
}