package com.closr.domain.fitting.dto;

// 최상위 응답 DTO 가상 피팅 및 사이즈별 추천 결과
public record ResponseFittingDto(
        Long garmentId,
        ResponseSizeOptionsDto sizes,
        String recommendedSize,
        String recommendationReason
) {}
