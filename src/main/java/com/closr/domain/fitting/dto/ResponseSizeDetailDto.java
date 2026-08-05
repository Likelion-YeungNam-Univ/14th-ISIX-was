package com.closr.domain.fitting.dto;

import java.util.List;

public record ResponseSizeDetailDto(
        String modelUrl, // 해당 사이즈 3D 모델 또는 피팅 결과 URL
        List<ResponseFitPartDto> parts, // 여유량 및 핏 판정 리스트
        boolean recommended // 추천 사이즈인지 여부
) {}
