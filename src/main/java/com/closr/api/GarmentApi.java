package com.closr.api;

import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Garment", description = "의류 카탈로그 API")
@RequestMapping("/api/v1/garments")
public interface GarmentApi {

    @Operation(summary = "의류 목록 조회", description = "피팅룸에서 선택 가능한 의류 목록을 반환합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<ResponseGarmentListDto>> getGarmentList();
}
