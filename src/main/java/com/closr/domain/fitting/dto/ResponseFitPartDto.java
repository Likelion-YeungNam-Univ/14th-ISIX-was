package com.closr.domain.fitting.dto;

// 개별 신체 부위별 피팅 계산 결과
public record ResponseFitPartDto(
        String part, // 부위 이름
        double ease, // 계산된 여유량
        String verdict // 핏 판정 결과
) {}