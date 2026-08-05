package com.closr.api;

import com.closr.domain.avatar.dto.ResponseAvatarJobDto;
import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Avatar", description = "아바타 생성 및 상태 조회 API")
@RequestMapping("/api/v1/avatars")
public interface AvatarApi {

    @Operation(summary = "아바타 생성 요청", description = "전신 사진과 신체 정보를 입력받아 아바타 생성을 비동기로 요청합니다. (최대 30초 소요)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<ResponseAvatarJobDto>> createAvatar(
            @Parameter(description = "전신 사진 파일 (JPEG/PNG, 10MB 이하)")
            @RequestPart("photo") MultipartFile photo,

            @Parameter(description = "키 (cm, 130~200)")
            @RequestParam("height") @Min(130) @Max(200) int height,

            @Parameter(description = "몸무게 (kg, 30~150)")
            @RequestParam("weight") @Min(30) @Max(150) int weight
    );

    @Operation(summary = "아바타 생성 상태 조회 (폴링)", description = "발급받은 jobId로 아바타 생성 진행 상태와 최종 치수 결과를 조회합니다.")
    @GetMapping("/{jobId}")
    ResponseEntity<ApiResponse<ResponseAvatarStatusDto>> getAvatarStatus(
            @Parameter(description = "아바타 생성 요청 시 발급받은 jobId")
            @PathVariable("jobId") String jobId
    );
}