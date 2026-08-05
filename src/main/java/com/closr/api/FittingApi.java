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
@RequestMapping("/api/v1/avatars/{avatarId}/garments/{garmentId}/fit")
public interface FittingApi {

    @Operation(
            summary = "사이즈별 여유량·판정 및 추천 사이즈 조회",
            description = "아바타와 의류 조합에 대해 S/M/L 사이즈별 부위별 여유량(ease)·판정(verdict)과 추천 사이즈를 한 번에 반환합니다."
    )
    @GetMapping
    ResponseEntity<ApiResponse<ResponseFittingDto>> getFitting(
            @Parameter(description = "아바타 ID") @PathVariable Long avatarId,
            @Parameter(description = "의류 ID") @PathVariable Long garmentId
    );
}