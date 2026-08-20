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
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "height_cm")
    private Integer height;

    @Column(name = "weight_kg")
    private Integer weight;

    @Column(name = "glb_url", length = 500)
    private String glbUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> measurements;

    private Double confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> warnings;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "body_type", length = 30)
    private String bodyType;

    @Column(name = "body_type_label", length = 20)
    private String bodyTypeLabel;

    @Column(name = "body_type_message", columnDefinition = "text")
    private String bodyTypeMessage;

    @Builder
    private Avatar(Session session, String jobId, String status,
                   Integer height, Integer weight, String glbUrl,
                   Map<String, Double> measurements, Double confidence,
                   List<String> warnings) {
        this.session = session;
        this.jobId = jobId;
        this.status = status;
        this.height = height;
        this.weight = weight;
        this.glbUrl = glbUrl;
        this.measurements = measurements;
        this.confidence = confidence;
        this.warnings = warnings;
    }

    public void markDone(String glbUrl, Map<String, Double> measurements, Double confidence,
                         List<String> warnings, String bodyType, String bodyTypeLabel, String bodyTypeMessage) {
        this.status = "done";
        this.glbUrl = glbUrl;
        this.measurements = measurements;
        this.confidence = confidence;
        this.warnings = warnings;
        this.bodyType = bodyType;
        this.bodyTypeLabel = bodyTypeLabel;
        this.bodyTypeMessage = bodyTypeMessage;
    }

    public void markFailed() {
        this.status = "failed";
    }

    public void updateName(String newName) {
        this.name = newName;
    }
}