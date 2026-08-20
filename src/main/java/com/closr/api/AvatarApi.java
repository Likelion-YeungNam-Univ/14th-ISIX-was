package com.closr.api;

import com.closr.domain.avatar.dto.ResponseAvatarJobDto;
import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Avatar", description = "아바타 생성 · 조회 · 삭제 API")
@RequestMapping("/api/v1/avatars")
public interface AvatarApi {

    @Operation(summary = "아바타 생성 요청", description = "전신 사진과 신체 정보를 입력받아 아바타 생성을 비동기로 요청합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<ResponseAvatarJobDto>> createAvatar(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "전신 사진 파일 (JPEG/PNG, 10MB 이하)")
            @RequestPart("photo") MultipartFile photo,
            @Parameter(description = "키 (cm, 130~200)")
            @Min(130) @Max(200)
            @RequestParam("height") int height,
            @Parameter(description = "몸무게 (kg, 30~150)")
            @Min(30) @Max(150)
            @RequestParam("weight") int weight
    );

    @Operation(summary = "아바타 생성 상태 조회 (폴링)", description = "발급받은 jobId로 아바타 생성 진행 상태와 최종 치수 결과를 조회합니다.")
    @GetMapping("/{jobId}")
    ResponseEntity<ApiResponse<ResponseAvatarStatusDto>> getAvatarStatus(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "아바타 생성 요청 시 발급받은 jobId")
            @PathVariable("jobId") String jobId
    );

    @Operation(summary = "내 아바타 목록 조회", description = "세션 토큰을 기반으로 지금까지 생성한 모든 아바타 목록을 최신순으로 불러옵니다.")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<List<ResponseAvatarStatusDto>>> getMyAvatars(
            @Parameter(hidden = true) @RequestAttribute("session") Session session
    );

    @Operation(summary = "아바타 삭제",
            description = "아바타와 그 아바타로 남긴 피팅 기록을 지웁니다. "
                    + "대화 기록은 지우지 않고 아바타 연결만 끊습니다 — 나눈 말과 "
                    + "취향 요약은 몸이 바뀌어도 유효합니다.")
    @DeleteMapping("/{avatarId}")
    ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "목록 조회로 받은 avatarId")
            @PathVariable("avatarId") Long avatarId
    );
}