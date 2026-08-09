package com.closr.domain.fitting.dto;

import java.util.List;

public record ResponseSizeDetailDto(
        String modelUrl,                  // 사전 계산 GLB 주소. 파일이 없으면 null
        List<ResponseFitPartDto> parts,   // 부위별 여유량 · 판정
        boolean recommended,              // 추천 사이즈인지
        boolean wearable                  // 꽉 끼는 부위가 없어 착용 가능한지
) {}
