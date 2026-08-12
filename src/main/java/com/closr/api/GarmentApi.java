package com.closr.api;

import com.closr.domain.garment.dto.ResponseGarmentDetailDto;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Garment", description = "의류 카탈로그 API")
@RequestMapping("/api/v1/garments")
public interface GarmentApi {

    @Operation(summary = "의류 목록 조회", description = "피팅룸에서 선택 가능한 의류 목록을 반환합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<ResponseGarmentListDto>> getGarmentList();

    @Operation(summary = "의류 상세 조회", description = "의류 하나의 상세 정보를 반환합니다. avatarId를 주면 사이즈별 착용 가능 여부가 포함됩니다.")
    @GetMapping("/{garmentId}")
    ResponseEntity<ApiResponse<ResponseGarmentDetailDto>> getGarmentDetail(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "의류 ID") @PathVariable Long garmentId,
            @Parameter(description = "아바타 ID (선택)") @RequestParam(required = false) Long avatarId
    );
}