package com.closr.domain.user.controller;

import com.closr.api.AuthApi;
import com.closr.domain.user.dto.ResponseSessionDto;
import com.closr.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 목 컨트롤러.
 *
 * <p><b>임시 스텁입니다.</b> DB · 서비스 없이 고정값만 반환합니다.
 * 프론트가 화면을 먼저 붙일 수 있도록 응답 형태만 맞춰둔 것으로,
 * 실제 세션 발급 로직이 들어오면 이 클래스는 대체됩니다.
 *
 * <p>매핑과 스웨거 명세는 {@link AuthApi} 에 있습니다.
 */
@RestController
public class AuthController implements AuthApi {

    /** 항상 같은 토큰을 돌려줍니다. 프론트가 값을 하드코딩해 두고 테스트할 수 있습니다. */
    private static final String MOCK_SESSION_TOKEN = "mock-session-token";

    @Override
    public ResponseEntity<ApiResponse<ResponseSessionDto>> createGuestSession() {
        return ResponseEntity.ok(
                ApiResponse.ok(new ResponseSessionDto(MOCK_SESSION_TOKEN))
        );
    }
}
