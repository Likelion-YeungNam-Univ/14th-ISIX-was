package com.closr.domain.fitting.service;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.fitting.dto.ResponseFittingRecordDetailDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피팅 기록 저장 · 조회.
 *
 * <p>피팅을 계산하는 쪽에서 {@link #save} 를 부르면 결과가 남고,
 * 이후 목록과 상세로 다시 꺼내볼 수 있습니다.
 *
 * <p>같은 조합을 여러 번 피팅하면 그때마다 기록이 쌓입니다.
 * 이전에 본 결과를 다시 열어보는 것이 목적이라 덮어쓰지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingRecordService {

    private final FittingRecordRepository fittingRecordRepository;

    /**
     * 피팅 결과를 기록합니다.
     *
     * @param result 피팅 응답 전체. 나중에 그대로 돌려주므로 요약하지 말고 통째로 넘기세요.
     */
    @Transactional
    public FittingRecord save(Session session, Avatar avatar, Garment garment,
                              String recommendedSize, boolean wearable,
                              Map<String, Object> result) {
        return fittingRecordRepository.save(FittingRecord.builder()
                .session(session)
                .avatar(avatar)
                .garment(garment)
                .recommendedSize(recommendedSize)
                .wearable(wearable)
                .result(result)
                .build());
    }

    /**
     * 세션의 피팅 기록을 최신순으로 반환합니다. <b>같은 옷은 최신 1건만 남깁니다.</b>
     *
     * <p>기록은 피팅을 조회할 때마다 쌓입니다. 그래서 같은 옷을 두 번 보면 두 건이
     * 되고, 사용자에게는 "입어본 옷이 전부 뜬다" 로 보입니다. 옷 하나에 대해 알고
     * 싶은 것은 "가장 최근에 어땠는지" 라 최신 것만 보여줍니다.
     *
     * <p>{@link com.closr.domain.chat.service.PastFittingReader} 가 챗봇에 넣을
     * 기록을 고를 때 쓰는 규칙과 같습니다. 두 곳이 다르면 <b>요약에 없는 옷을
     * 챗봇이 말하는</b> 상황이 생깁니다.
     *
     * <p>행 자체는 지우지 않습니다. 그때 본 판정을 그대로 돌려주는 것이
     * {@link #findOne} 의 계약이고, 과거 기록을 조회로 없앨 이유가 없습니다.
     */
    public ResponseFittingRecordListDto findMine(Session session) {
        Set<Long> seen = new LinkedHashSet<>();
        List<ResponseFittingRecordDto> fittings = new ArrayList<>();

        for (FittingRecord record : fittingRecordRepository.findAllBySessionWithGarment(session)) {
            // 조회가 최신순이므로 먼저 만난 것이 그 옷의 최신 기록입니다.
            if (!seen.add(record.getGarment().getId())) {
                continue;
            }
            fittings.add(new ResponseFittingRecordDto(
                    record.getId(),
                    record.getAvatar().getId(),
                    record.getGarment().getId(),
                    record.getGarment().getName(),
                    record.getRecommendedSize(),
                    record.isWearable(),
                    record.getCreatedAt()));
        }

        return new ResponseFittingRecordListDto(fittings);
    }

    /**
     * 피팅 기록을 지웁니다. <b>같은 옷의 기록을 함께 지웁니다.</b>
     *
     * <p>목록이 옷마다 최신 1건만 보여주므로, 지목된 행만 지우면 숨어 있던 예전
     * 기록이 그 자리에 다시 올라옵니다. 사용자에게는 "지웠는데 그대로 있다" 로
     * 보입니다. 그래서 보이는 항목 하나가 사라지도록 같은 옷을 묶어 지웁니다.
     *
     * <p>아바타까지 같은 것만 지웁니다. 아바타를 다시 만들었다면 다른 몸의
     * 기록이라 남겨 두는 것이 맞습니다.
     *
     * <p>남의 기록이면 403 이 아니라 404 입니다. 403 은 "그 기록이 있다" 를
     * 알려주므로 존재 여부까지 감춥니다.
     */
    @Transactional
    public void delete(Session session, Long fittingId) {
        FittingRecord record = fittingRecordRepository.findById(fittingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FITTING_NOT_FOUND));

        if (!record.belongsTo(session)) {
            throw new CustomException(ErrorCode.FITTING_NOT_FOUND);
        }

        int removed = fittingRecordRepository.deleteBySessionAndAvatarAndGarment(
                session, record.getAvatar(), record.getGarment());
        log.debug("피팅 기록 {}건을 지웠습니다. fittingId={}", removed, fittingId);
    }

    /**
     * 저장된 피팅 결과를 그대로 반환합니다.
     *
     * <p>다시 계산하지 않습니다. 판정 기준이 바뀌어도 그때 본 화면이 나와야 합니다.
     */
    public ResponseFittingRecordDetailDto findOne(Session session, Long fittingId) {
        FittingRecord record = fittingRecordRepository.findById(fittingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FITTING_NOT_AVAILABLE));

        if (!record.belongsTo(session)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return new ResponseFittingRecordDetailDto(
                record.getId(),
                record.getAvatar().getId(),
                record.getGarment().getId(),
                record.getGarment().getName(),
                record.getCreatedAt(),
                record.getResult());
    }
}
