package com.closr.domain.garment.entity;

import com.closr.domain.user.entity.Session;
import com.closr.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 사용자가 찜한 의류.
 *
 * <p>세션 하나가 같은 의류를 두 번 찜할 수 없도록 (session_id, garment_id) 에
 * unique 제약을 둡니다.
 */
@Entity
@Table(
        name = "garment_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_garment_likes_session_garment",
                columnNames = {"session_id", "garment_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GarmentLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    @Builder
    private GarmentLike(Session session, Garment garment) {
        this.session = session;
        this.garment = garment;
    }
}
