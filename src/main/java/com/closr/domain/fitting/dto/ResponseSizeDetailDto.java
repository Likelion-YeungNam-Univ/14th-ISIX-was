package com.closr.domain.fitting.dto;

import java.util.List;

/**
 * 사이즈 하나의 피팅 결과.
 *
 * <p>{@code penalty} 와 {@code totalDev} 는 추천 사이즈를 고른 근거입니다.
 * 착용 가능한 사이즈 중 penalty 가 작은 쪽, 같으면 totalDev 가 작은 쪽을 추천합니다.
 */
public record ResponseSizeDetailDto(
        String modelUrl,                  // 사전 계산 GLB 주소. 파일이 없으면 null
        List<ResponseFitPartDto> parts,   // 부위별 여유량 · 판정
        double penalty,                   // 허용 범위를 벗어난 정도. 범위 안이면 0
        double totalDev,                  // 편차 절대값 합
        boolean wearable,                 // 착용 가능 여부. "꽉 낌" 부위가 있으면 false
        boolean recommended               // 추천 사이즈인지
) {}
