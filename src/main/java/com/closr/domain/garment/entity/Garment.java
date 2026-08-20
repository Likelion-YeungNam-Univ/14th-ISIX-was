package com.closr.domain.garment.entity;

import com.closr.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피팅룸에서 고를 수 있는 의류.
 *
 * <p>사전 계산한 시뮬레이션 결과를 조회하는 구조라, 여기 등록된 의류만
 * 피팅할 수 있습니다. 현재 6종입니다.
 */
@Entity
@Table(name = "garments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Garment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 디자인 코드. 예) tshirt_basic
     *
     * <p>사전 계산한 GLB 파일명과 AI 파트가 쓰는 식별자가 이 값이라
     * 사람이 읽는 이름과 분리해 둡니다. 시드 데이터의 기준 키입니다.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String design;

    @Column(nullable = false, length = 100)
    private String name;

    /** top · bottom · dress */
    @Column(nullable = false, length = 30)
    private String category;

    /**
     * 핏. 슬림 · 레귤러 · 오버핏.
     *
     * <p>같은 여유량이라도 핏에 따라 판정이 달라집니다. 오버핏 가슴 여유 20cm 는
     * 정상이지만 슬림에서는 과대입니다. 그래서 허용 편차 범위를 핏별로 다르게 두고,
     * 이 값으로 {@code fit_tolerances} 를 찾습니다.
     *
     * <p>기존 행에도 값을 채워야 해서 not null 을 걸지 않았습니다.
     */
    @Column(length = 20)
    private String fit;

    /**
     * 목록 화면에 쓰는 썸네일 주소.
     *
     * <p>이미지가 아직 준비되지 않아 시드에서는 비워 둡니다.
     * 스토리지가 붙으면 채웁니다.
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "purchase_url", length = 500)
    private String purchaseUrl;

    @Builder
    private Garment(String design, String name, String category, String fit, String thumbnailUrl, String purchaseUrl) {
        this.design = design;
        this.name = name;
        this.category = category;
        this.fit = fit;
        this.thumbnailUrl = thumbnailUrl;
        this.purchaseUrl = purchaseUrl;
    }
}
