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

    /**
     * 아바타를 지웁니다. 딸린 것을 먼저 정리합니다.
     *
     * <p><b>순서가 있습니다.</b> 아바타를 먼저 지우면 참조가 남은 채로 외래키가
     * 걸려 실패합니다.
     *
     * <pre>
     *   ① 피팅 기록   지웁니다   — 판정을 다시 계산할 몸이 없어집니다
     *   ② 대화        연결만 끊습니다 — 나눈 말과 취향 요약은 남깁니다
     *   ③ 아바타
     * </pre>
     *
     * <p>대화를 지우지 않는 것은 판단입니다. 요약에 담긴 용도 · 선호 핏은 몸이
     * 바뀌어도 유효하고, 사용자가 나눈 말을 아바타 삭제로 함께 잃으면 되돌릴
     * 방법이 없습니다.
     *
     * <p>3D 파일은 지우지 않습니다. R2 에 남지만 주소를 아는 곳이 없어져
     * 화면에 뜨지 않습니다. 저장소 정리는 이 API 의 일이 아닙니다.
     */
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

    /**
     * 내 아바타를 꺼냅니다. 남의 것이면 거절합니다.
     *
     * <p>없는 것과 남의 것을 다른 코드로 답합니다. 둘 다 404 로 뭉개면 프론트가
     * 화면을 어떻게 되돌릴지 정할 수 없습니다 — 없는 것은 목록을 다시 받아야
     * 하고, 권한 문제는 세션이 바뀐 것입니다.
     */
    private Avatar readOwned(Session session, Long avatarId) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));

        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return avatar;
    }
}
