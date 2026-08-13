package com.closr.domain.garment.controller;

import com.closr.api.GarmentApi;
import com.closr.domain.garment.dto.ResponseGarmentDetailDto;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.garment.service.GarmentService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GarmentController implements GarmentApi {

    private final GarmentService garmentService;

    @Override
    public ResponseEntity<ApiResponse<ResponseGarmentListDto>> getGarmentList(String sort) {
        return ResponseEntity.ok(ApiResponse.ok(garmentService.getGarmentList(sort)));
    }

    @Override
    public ResponseEntity<ApiResponse<ResponseGarmentDetailDto>> getGarmentDetail(
            @RequestAttribute("session") Session session, Long garmentId, Long avatarId) {
        return ResponseEntity.ok(ApiResponse.ok(
                garmentService.getGarmentDetail(session, garmentId, avatarId)));
    }
}