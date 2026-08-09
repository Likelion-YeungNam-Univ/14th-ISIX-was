package com.closr.domain.fitting.dto;

/**
 * 부위 하나의 피팅 결과.
 *
 * <p>판정만 주면 사용자가 납득하기 어렵습니다. 여유가 20cm 인데 "적정" 이라고만
 * 하면 이상해 보이므로, 목표 여유와 편차를 함께 내보내 근거를 보여줍니다.
 *
 * <p>{@code part} 는 아바타 계측({@code measurements})과 같은 키를 씁니다.
 * 프론트가 두 응답을 그대로 매칭할 수 있도록 하기 위함입니다.
 * 예) {@code chest_circ} · {@code waist_circ} · {@code hip_circ} · {@code shoulder_width}
 */
public record ResponseFitPartDto(
        String part,       // 부위 이름 (아바타 계측과 같은 키)
        double actualEase, // 실제 여유 = 의류 치수 - 아바타 치수
        double refEase,    // 이 옷이 의도한 목표 여유
        double deviation,  // 편차 = 실제 여유 - 목표 여유
        String verdict,    // 적정 · 꽉 낌 · 여유 있음
        String color       // green · red · blue
) {}
