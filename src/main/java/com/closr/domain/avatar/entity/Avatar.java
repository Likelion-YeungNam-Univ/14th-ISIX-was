package com.closr.domain.avatar.entity;

import com.closr.domain.user.entity.Session;
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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 사진 1장으로 만든 3D 아바타.
 *
 * <p>생성은 AI 서버에서 비동기로 처리하므로 상태를 함께 들고 있습니다.
 * status 가 done 이 되면 glbUrl 과 measurements 가 채워집니다.
 */
@Entity
@Table(name = "avatars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Avatar extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** AI 서버가 발급한 작업 식별자. 폴링에 사용합니다. */
    @Column(name = "job_id", length = 64)
    private String jobId;

    /** processing · done · failed */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "height_cm")
    private Integer height;

    @Column(name = "weight_kg")
    private Integer weight;

    @Column(name = "glb_url", length = 500)
    private String glbUrl;

    /**
     * 부위별 실측 치수 (cm).
     *
     * <p>키 12개는 AI 파트가 정의한 문자열을 그대로 씁니다.
     * 부위가 늘거나 줄어도 스키마를 바꾸지 않으려고 jsonb 로 둡니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> measurements;

    /** AI 서버가 보고한 인식 신뢰도. */
    private Double confidence;

    @Builder
    private Avatar(Session session, String jobId, String status,
                   Integer height, Integer weight, String glbUrl,
                   Map<String, Double> measurements, Double confidence) {
        this.session = session;
        this.jobId = jobId;
        this.status = status;
        this.height = height;
        this.weight = weight;
        this.glbUrl = glbUrl;
        this.measurements = measurements;
        this.confidence = confidence;
    }
    public void markDone(String glbUrl, Map<String, Double> measurements, Double confidence) {
        this.status = "done";
        this.glbUrl = glbUrl;
        this.measurements = measurements;
        this.confidence = confidence;
    }

    public void markFailed() {
        this.status = "failed";
    }
}
