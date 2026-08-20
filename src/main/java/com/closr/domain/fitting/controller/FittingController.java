package com.closr.domain.fitting.controller;

import com.closr.api.FittingApi;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.service.FittingService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가상 피팅 컨트롤러.
 *
 * <p>매핑과 스웨거 명세는 {@link FittingApi} 에 있습니다.
 */
@RestController
@RequiredArgsConstructor
public class FittingController implements FittingApi {

    private final FittingService fittingService;

    @Override
    public ResponseEntity<ApiResponse<ResponseFittingDto>> getFitting(
            @RequestAttribute("session") Session session, Long avatarId, Long garmentId) {
        return ResponseEntity.ok(
                ApiResponse.ok(fittingService.getFitting(session, avatarId, garmentId)));
    }
}
