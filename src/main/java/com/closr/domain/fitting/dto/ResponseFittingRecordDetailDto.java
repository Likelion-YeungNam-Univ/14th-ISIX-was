package com.closr.domain.fitting.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 저장된 피팅 결과 상세.
 *
 * <p>{@code result} 는 피팅 당시의 응답을 그대로 담고 있습니다.
 * 다시 계산하지 않으므로 판정 기준이 바뀌어도 그때 본 화면이 그대로 나옵니다.
 */
public record ResponseFittingRecordDetailDto(
        Long fittingId,
        Long avatarId,
        Long garmentId,
        String garmentName,
        LocalDateTime fittedAt,
        Map<String, Object> result
) {}
