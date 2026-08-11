package com.closr.domain.garment.controller;

import com.closr.api.LikeApi;
import com.closr.domain.garment.dto.ResponseLikeDto;
import com.closr.domain.garment.service.LikeService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LikeController implements LikeApi {

    private final LikeService likeService;

    @Override
    public ResponseEntity<ApiResponse<Void>> like(Session session, Long garmentId) {
        likeService.like(session, garmentId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> unlike(Session session, Long garmentId) {
        likeService.unlike(session, garmentId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ResponseLikeDto>>> getMyLikes(Session session) {
        return ResponseEntity.ok(ApiResponse.ok(likeService.getMyLikes(session)));
    }
}
