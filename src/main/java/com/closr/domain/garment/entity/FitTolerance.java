package com.closr.domain.garment.entity;

import com.closr.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핏 · 부위별 허용 편차 범위.
 *
 * <p>목표 여유 대비 편차가 이 범위 안이면 적정입니다.
 * 아래보다 작으면 꽉 끼고, 위보다 크면 여유 있는 것으로 봅니다.
 *
 * <pre>
 *   편차 &lt; devMin   꽉 낌      (착용 불가 판정에 사용)
 *   devMin ~ devMax  적정
 *   편차 &gt; devMax   여유 있음  (착용은 가능)
 * </pre>
 *
 * <p>핏에만 종속되고 의류마다 다르지 않아 별도 테이블로 둡니다.
 * 의류가 늘어도 이 표는 그대로입니다.
 *
 * <p>어깨는 다른 부위보다 허용 폭이 훨씬 좁습니다(레귤러 기준 -1~+2).
 * 어깨가 맞지 않으면 다른 곳이 맞아도 못 입기 때문입니다.
 */
@Entity
@Table(
        name = "fit_tolerances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fit_tolerance",
                columnNames = {"fit", "part"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FitTolerance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 슬림 · 레귤러 · 오버핏 */
    @Column(nullable = false, length = 20)
    private String fit;

    /** shoulder_width · chest_circ · waist_circ · hip_circ */
    @Column(nullable = false, length = 30)
    private String part;

    @Column(name = "dev_min", nullable = false)
    private Double devMin;

    @Column(name = "dev_max", nullable = false)
    private Double devMax;

    @Builder
    private FitTolerance(String fit, String part, Double devMin, Double devMax) {
        this.fit = fit;
        this.part = part;
        this.devMin = devMin;
        this.devMax = devMax;
    }

    /** 편차가 허용 범위 안인지. */
    public boolean contains(double deviation) {
        return devMin <= deviation && deviation <= devMax;
    }

    /** 허용 범위보다 작은지. 꽉 끼는 경우로, 착용 불가 판정에 씁니다. */
    public boolean isTight(double deviation) {
        return deviation < devMin;
    }
}
