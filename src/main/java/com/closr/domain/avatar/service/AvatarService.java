package com.closr.domain.avatar.service;

import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import com.closr.infra.ai.AiAvatarJobResponse;
import com.closr.infra.ai.AiAvatarResponse;
import com.closr.infra.ai.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvatarService {

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_DONE = "done";
    private static final String STATUS_FAILED = "failed";

    private final AvatarRepository avatarRepository;
    private final AiClient aiClient;

    @Transactional
    public Avatar requestAvatar(Session session, MultipartFile photo, int height, int weight) {
        AiAvatarJobResponse response = aiClient.requestAvatar(photo, height, weight);

        // NPE 방어: AI 서버 응답이 실패했거나 데이터가 비어있으면 터지기 전에 예외 던지기!
        if (!response.success() || response.data() == null) {
            log.error("AI 서버 아바타 생성 요청 실패 - session: {}", session.getId());
            throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
        }

        AiAvatarJobResponse.Data data = response.data();

        Avatar avatar = Avatar.builder()
                .session(session)
                .jobId(data.avatarId())
                .status(data.status())
                .height(height)
                .weight(weight)
                .build();

        return avatarRepository.save(avatar);
    }

    @Transactional
    public Avatar getAvatarStatus(Session session, String jobId) {
        Avatar avatar = avatarRepository.findByJobId(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (STATUS_PROCESSING.equals(avatar.getStatus())) {
            syncFromAiServer(avatar);
        }

        return avatar;
    }

    private void syncFromAiServer(Avatar avatar) {
        AiAvatarResponse response = aiClient.getAvatar(avatar.getJobId());

        // NPE 방어: 폴링 중 AI 서버가 일시적으로 이상한 응답을 주면
        // 터지지 않게 이번 턴은 그냥 넘기고 다음 폴링을 노림
        if (!response.success() || response.data() == null) {
            log.warn("AI 서버 상태 조회 일시적 실패 - jobId: {}", avatar.getJobId());
            return;
        }

        AiAvatarResponse.Data data = response.data();

        if (response.isDone()) {
            AiAvatarResponse.Result result = data.result();
            if (result != null) {
                avatar.markDone(result.glbUrl(), result.measurements(), result.confidence(), result.warnings(),
                        result.bodyType(), result.bodyTypeLabel(), result.bodyTypeMessage());
            }
        } else if (STATUS_FAILED.equals(data.status())) {
            avatar.markFailed();
        }
        // processing이면 그대로 둠
    }

    @Transactional
    public List<Avatar> getMyAvatars(Session session) {
        // 세션에 해당하는 모든 아바타 최신순으로 싹 다 가져오기
        List<Avatar> avatars = avatarRepository.findAllBySessionOrderByCreatedAtDesc(session);

        return avatars;
    }

    @Transactional
    public void updateName(Session session, Long avatarId, String newName) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));
        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.AVATAR_NOT_FOUND);
        }

        avatar.updateName(newName);
    }
}