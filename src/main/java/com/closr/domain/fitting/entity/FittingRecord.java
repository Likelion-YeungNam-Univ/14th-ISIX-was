package com.closr.domain.fitting.entity;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.garment.entity.Garment;
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
 * 피팅 결과 기록.
 *
 * <p>사용자가 본 피팅 결과를 그대로 남겨, 나중에 다시 열어볼 수 있게 합니다.
 *
 * <p><b>판정 결과를 통째로 저장합니다.</b> 추천 사이즈만 남기면 그때 본 화면을
 * 복원할 수 없습니다. 의류 스펙이나 판정 기준이 나중에 바뀌어도 기록은 그대로여야
 * 하므로, 다시 계산하지 않고 저장된 값을 그대로 돌려줍니다.
 *
 * <p>회원 개념이 없어 세션에 매답니다. 세션이 만료되면(발급 후 7일) 기록도
 * 접근할 수 없습니다. 프론트가 세션 토큰을 보관해야 재방문 시 복원됩니다.
 */
@Entity
@Table(name = "fitting_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id", nullable = false)
    private Avatar avatar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    /** 그때 추천된 사이즈. 목록에서 다시 계산하지 않으려고 따로 둡니다. */
    @Column(name = "recommended_size", length = 10)
    private String recommendedSize;

    /** 착용 가능한 사이즈가 하나라도 있었는지. 목록에서 바로 보여줍니다. */
    @Column(nullable = false)
    private boolean wearable;

    /**
     * 피팅 응답 전체.
     *
     * <p>사이즈별 여유량 · 편차 · 판정이 모두 들어갑니다. 판정 스펙이 바뀌어도
     * 저장 시점의 결과를 그대로 보여주기 위해 통째로 담습니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> result;

    @Builder
    private FittingRecord(Session session, Avatar avatar, Garment garment,
                          String recommendedSize, boolean wearable, Map<String, Object> result) {
        this.session = session;
        this.avatar = avatar;
        this.garment = garment;
        this.recommendedSize = recommendedSize;
        this.wearable = wearable;
        this.result = result;
    }

    /** 이 기록이 해당 세션의 것인지. 다른 세션의 기록은 열어볼 수 없습니다. */
    public boolean belongsTo(Session other) {
        return session.getId().equals(other.getId());
    }
}
