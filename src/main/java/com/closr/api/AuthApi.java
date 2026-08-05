package com.closr.api;

import com.closr.domain.user.dto.ResponseSessionDto;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "인증 및 세션 API")
@RequestMapping("/api/v1/sessions")
public interface AuthApi {

    @Operation(summary = "ㅇ게스트 세션 발급", description = "가입 없이 체험하기 위한 7일 유지 세션 토큰을 발급합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ResponseSessionDto>> createGuestSession();
}