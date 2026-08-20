package com.closr.domain.user.controller;

import com.closr.api.AuthApi;
import com.closr.domain.user.dto.ResponseSessionDto;
import com.closr.domain.user.service.SessionService;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final SessionService sessionService;

    @Override
    public ResponseEntity<ApiResponse<ResponseSessionDto>> createGuestSession() {
        String realSessionToken = sessionService.createGuestSession();

        return ResponseEntity.ok(
                ApiResponse.ok(new ResponseSessionDto(realSessionToken))
        );
    }
}