package com.closr.domain.garment.controller;

import com.closr.api.GarmentApi;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.garment.service.GarmentService;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 의류 카탈로그 컨트롤러.
 *
 * <p>매핑과 스웨거 명세는 {@link GarmentApi} 에 있습니다.
 */
@RestController
@RequiredArgsConstructor
public class GarmentController implements GarmentApi {

    private final GarmentService garmentService;

    @Override
    public ResponseEntity<ApiResponse<ResponseGarmentListDto>> getGarmentList() {
        return ResponseEntity.ok(ApiResponse.ok(garmentService.getGarmentList()));
    }
}
