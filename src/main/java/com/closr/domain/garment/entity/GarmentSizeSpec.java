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
 * <p>여유량(ease)은 아바타 치수에서 이 스펙을 빼서 계산합니다.
 * 값을 파일에서 읽지 않고 테이블로 들고 있는 이유는, 런타임에 CSV 를 읽으면
 * 배포 환경에 파일이 없거나 인코딩이 달라 깨지기 때문입니다.
 *
 * <p><b>스펙 확정 전이라 시드 데이터는 비어 있습니다.</b>
 * 부위 구성이 아직 바뀔 수 있어 measurements 를 jsonb 로 둡니다.
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

    /**
     * 부위별 목표 여유 (cm). 이 옷이 의도한 여유입니다.
     *
     * <p>판정은 실제 여유가 아니라 <b>목표 대비 편차</b>로 합니다.
     * <pre>
     *   실제 여유 = 의류 치수 - 아바타 치수
     *   편차      = 실제 여유 - 목표 여유
     * </pre>
     * 실제 여유만 보면 오버핏 20cm 와 슬림 20cm 를 구분할 수 없습니다.
     *
     * <p>사이즈마다 값이 다를 수 있어(슬랙스 허리 2.5 / 2.1 / 1.8)
     * 의류가 아니라 사이즈에 답니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_ease", columnDefinition = "jsonb")
    private Map<String, Double> targetEase;

    // GLB 주소를 여기 저장하지 않습니다. 파일이 체형 구간마다 달라
    // {design}_{size}__{bucket}.glb 로 요청 때 조합해야 하는데, 컬럼에는 구간을 담을
    // 자리가 없습니다. 실제로 시드가 이 컬럼을 채운 적이 없어 항상 null 이었습니다.
    // 조합은 GarmentAssetResolver 가 합니다.

    @Builder
    private GarmentSizeSpec(Garment garment, String size, Map<String, Double> measurements,
                            Map<String, Double> targetEase) {
        this.garment = garment;
        this.size = size;
        this.measurements = measurements;
        this.targetEase = targetEase;
    }
}
