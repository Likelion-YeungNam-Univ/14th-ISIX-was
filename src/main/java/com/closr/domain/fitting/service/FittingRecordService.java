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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

    /** 세션의 피팅 기록을 최신순으로 반환합니다. */
    public ResponseFittingRecordListDto findMine(Session session) {
        List<ResponseFittingRecordDto> fittings =
                fittingRecordRepository.findAllBySessionWithGarment(session).stream()
                        .map(record -> new ResponseFittingRecordDto(
                                record.getId(),
                                record.getAvatar().getId(),
                                record.getGarment().getId(),
                                record.getGarment().getName(),
                                record.getRecommendedSize(),
                                record.isWearable(),
                                record.getCreatedAt()))
                        .toList();

        return new ResponseFittingRecordListDto(fittings);
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
