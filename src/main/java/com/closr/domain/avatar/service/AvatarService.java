package com.closr.domain.avatar.service;

import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.chat.repository.ConversationRepository;
import com.closr.domain.fitting.repository.FittingRecordRepository;
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
    private final FittingRecordRepository fittingRecordRepository;
    private final ConversationRepository conversationRepository;
    private final AiClient aiClient;

    @Transactional
    public Avatar requestAvatar(Session session, MultipartFile photo, int height, int weight) {
        AiAvatarJobResponse response = aiClient.requestAvatar(photo, height, weight);

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
    }

    @Transactional
    public List<Avatar> getMyAvatars(Session session) {
        return avatarRepository.findAllBySessionOrderByCreatedAtDesc(session);
    }

    @Transactional
    public void updateName(Session session, Long avatarId, String newName) {
        Avatar avatar = readOwned(session, avatarId);
        avatar.updateName(newName);
    }

    @Transactional
    public void delete(Session session, Long avatarId) {
        Avatar avatar = readOwned(session, avatarId);

        int records = fittingRecordRepository.deleteBySessionAndAvatar(session, avatar);

        List<Conversation> conversations = conversationRepository.findAllByAvatar(avatar);
        for (Conversation conversation : conversations) {
            conversation.unlinkAvatar();
        }

        avatarRepository.delete(avatar);

        log.info("아바타 {} 삭제 - 피팅 기록 {}건, 대화 연결 {}건 정리",
                avatarId, records, conversations.size());
    }

    private Avatar readOwned(Session session, Long avatarId) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));

        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return avatar;
    }
}