package com.closr.domain.fitting.dto;

import com.closr.domain.garment.UnavailableReason;
import java.util.List;

/**
 * 사이즈 하나의 피팅 결과.
 *
 * <p>{@code penalty} 와 {@code totalDev} 는 추천 사이즈를 고른 근거입니다.
 * 착용 가능한 사이즈 중 penalty 가 작은 쪽, 같으면 totalDev 가 작은 쪽을 추천합니다.
 *
 * <p>{@code glbUrl} 과 {@code easeUrl} 은 사전 계산된 R2 주소입니다. 미리보기를 만들 수
 * 없는 조합에서는 둘 다 {@code null} 이고 {@code unavailableReason} 에 이유가 들어갑니다.
 * <b>{@code glbUrl} 이 {@code null} 인 채로 3D 뷰어에 넘기면 로더가 에러를 던지므로,
 * 프론트는 {@code if (unavailableReason)} 로 반드시 분기해야 합니다.</b>
 * 판정({@code parts})은 미리보기가 없어도 그대로 채워집니다.
 */
public record ResponseSizeDetailDto(
        String glbUrl,                        // 착용 메시 주소. 미제공 조합이면 null
        String easeUrl,                       // 히트맵이 읽는 여유량 파일. glbUrl 과 같은 조건
        UnavailableReason unavailableReason,   // 미리보기가 없는 이유. 정상이면 null
        List<ResponseFitPartDto> parts,        // 부위별 여유량 · 판정
        double penalty,                        // 허용 범위를 벗어난 정도. 범위 안이면 0
        double totalDev,                       // 편차 절대값 합
        boolean wearable,                      // 착용 가능 여부. "꽉 낌" 부위가 있으면 false
        boolean recommended                    // 추천 사이즈인지
) {}
