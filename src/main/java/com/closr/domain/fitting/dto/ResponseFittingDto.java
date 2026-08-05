package com.closr.domain.fitting.dto;

import java.util.Map;

public record ResponseFittingDto(
        String recommendedSize,
        String recommendReason,
        Map<String, Double> clearance // 부위별 여유량 (어깨, 가슴, 허리 등)
) {}