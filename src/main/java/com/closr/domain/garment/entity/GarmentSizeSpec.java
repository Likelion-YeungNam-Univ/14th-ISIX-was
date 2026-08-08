package com.closr.domain.garment.entity;

import com.closr.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 의류 한 벌의 사이즈별 실측 스펙.
 *
 * <p>여유량(ease)은 <b>이 스펙에서 아바타 치수를 빼서</b> 계산합니다.
 * 양수면 옷이 더 큰 것, 즉 여유입니다.
 * 값을 파일에서 읽지 않고 테이블로 들고 있는 이유는, 런타임에 CSV 를 읽으면
 * 배포 환경에 파일이 없거나 인코딩이 달라 깨지기 때문입니다.
 *
 * <p>부위는 검증이 끝난 4개만 씁니다.
 * {@code shoulder_width} · {@code chest_circ} · {@code waist_circ} · {@code hip_circ}.
 * {@code sleeve_length} · {@code back_length} 는 패턴에서 측정은 되지만
 * 아바타 계측과 정의가 같은지 확인되지 않아 제외했습니다.
 *
 * <p>의류마다 해당 부위가 달라(바지에 가슴둘레는 무의미) measurements 를 jsonb 로 둡니다.
 */
@Entity
@Table(
        name = "garment_size_specs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_garment_size",
                columnNames = {"garment_id", "size"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GarmentSizeSpec extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    /** S · M · L */
    @Column(nullable = false, length = 10)
    private String size;

    /** 부위별 실측 치수 (cm). 키는 아바타 치수와 같은 이름을 씁니다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> measurements;

    /** 해당 사이즈의 사전 계산 GLB 주소. */
    @Column(name = "model_url", length = 500)
    private String modelUrl;

    @Builder
    private GarmentSizeSpec(Garment garment, String size,
                            Map<String, Double> measurements, String modelUrl) {
        this.garment = garment;
        this.size = size;
        this.measurements = measurements;
        this.modelUrl = modelUrl;
    }
}
