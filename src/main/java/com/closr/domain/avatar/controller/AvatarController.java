package com.closr.domain.avatar.controller;

import com.closr.api.AvatarApi;
import com.closr.domain.avatar.dto.ResponseAvatarJobDto;
import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.service.AvatarService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 아바타 컨트롤러.
 *
 * <p>매핑과 스웨거 명세는 {@link AvatarApi} 에 있습니다.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class AvatarController implements AvatarApi {

    private final AvatarService avatarService;

    @Override
    public ResponseEntity<ApiResponse<ResponseAvatarJobDto>> createAvatar(
            @RequestAttribute("session") Session session,
            MultipartFile photo, int height, int weight) {

        Avatar avatar = avatarService.requestAvatar(session, photo, height, weight);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(new ResponseAvatarJobDto(avatar.getJobId())));
    }

    @Override
    public ResponseEntity<ApiResponse<ResponseAvatarStatusDto>> getAvatarStatus(
            @RequestAttribute("session") Session session,
            String jobId) {

        Avatar avatar = avatarService.getAvatarStatus(session, jobId);

        return ResponseEntity.ok(ApiResponse.ok(new ResponseAvatarStatusDto(
                avatar.getStatus(),
                avatar.getId(),
                avatar.getMeasurements()
        )));
    }
}