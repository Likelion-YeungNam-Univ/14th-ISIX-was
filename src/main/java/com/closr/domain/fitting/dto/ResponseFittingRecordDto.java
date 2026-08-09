package com.closr.domain.fitting.dto;

import java.time.LocalDateTime;

/** 피팅 기록 목록의 한 줄. 상세는 fittingId 로 다시 조회합니다. */
public record ResponseFittingRecordDto(
        Long fittingId,
        Long avatarId,
        Long garmentId,
        String garmentName,
        String recommendedSize,
        boolean wearable,
        LocalDateTime fittedAt
) {}
