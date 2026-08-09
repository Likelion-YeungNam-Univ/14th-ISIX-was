package com.closr.domain.fitting;

import com.closr.domain.garment.entity.FitTolerance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 부위별 핏 판정.
 *
 * <p>목표 여유 대비 편차를 핏별 허용 범위와 비교해 정합니다.
 * 값과 색상은 시뮬레이션 파트의 {@code fit_judge.py} 와 같습니다.
 */
@Getter
@RequiredArgsConstructor
public enum FitVerdict {

    /** 허용 범위보다 작습니다. 이 부위가 하나라도 있으면 착용 불가로 봅니다. */
    TIGHT("꽉 낌", "red"),

    /** 허용 범위 안입니다. */
    GOOD("적정", "green"),

    /** 허용 범위보다 큽니다. 헐렁하지만 입을 수는 있습니다. */
    LOOSE("여유 있음", "blue");

    private final String label;
    private final String color;

    public static FitVerdict of(double deviation, FitTolerance tolerance) {
        if (tolerance.isTight(deviation)) {
            return TIGHT;
        }
        return tolerance.contains(deviation) ? GOOD : LOOSE;
    }
}
