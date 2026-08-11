package com.closr.api;

import com.closr.domain.garment.dto.ResponseLikeDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like", description = "의류 찜하기 API")
public interface LikeApi {

    @Operation(summary = "의류 찜하기")
    @PostMapping("/api/v1/garments/{id}/like")
    ResponseEntity<ApiResponse<Void>> like(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @PathVariable("id") Long garmentId
    );

    @Operation(summary = "찜 취소")
    @DeleteMapping("/api/v1/garments/{id}/like")
    ResponseEntity<ApiResponse<Void>> unlike(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @PathVariable("id") Long garmentId
    );

    @Operation(summary = "내가 찜한 의류 목록 조회")
    @GetMapping("/api/v1/likes/me")
    ResponseEntity<ApiResponse<List<ResponseLikeDto>>> getMyLikes(
            @Parameter(hidden = true) @RequestAttribute("session") Session session
    );
}
