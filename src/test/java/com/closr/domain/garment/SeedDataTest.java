package com.closr.domain.garment;

import static org.assertj.core.api.Assertions.assertThat;

import com.closr.domain.garment.entity.FitTolerance;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.FitToleranceRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시드 데이터 검증.
 *
 * <p>data.sql 은 CSV 에서 생성한 것이라 사람이 눈으로 검토하기 어렵습니다.
 * 판정 로직이 기대하는 형태가 실제로 들어갔는지 여기서 확인합니다.
 *
 * <p>부위 하나만 비어도 그 의류는 판정이 되지 않으므로 값의 존재까지 봅니다.
 *
 * <p>{@code open-in-view: false} 라 {@code spec.getGarment()} 같은 지연 로딩은
 * 트랜잭션 안에서만 됩니다. 판정 로직을 구현할 때도 같은 제약을 받습니다.
 */
@SpringBootTest
@Transactional
class SeedDataTest {

    private static final List<String> DESIGNS = List.of(
            "tshirt_basic", "shirt_slim", "shirt_over",
            "dress_basic", "pants_slacks", "skirt_pencil");

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private GarmentSizeSpecRepository garmentSizeSpecRepository;

    @Autowired
    private FitToleranceRepository fitToleranceRepository;

    @Test
    @DisplayName("의류 6종이 모두 핏을 가진다")
    void everyGarmentHasFit() {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();

        assertThat(garments).hasSize(6);
        assertThat(garments).extracting(Garment::getDesign).containsExactlyElementsOf(DESIGNS);
        assertThat(garments).allSatisfy(garment ->
                assertThat(garment.getFit()).isIn("슬림", "레귤러", "오버핏"));
    }

    @Test
    @DisplayName("의류마다 S · M · L 스펙이 있고 실측 치수와 목표 여유가 같은 부위를 담는다")
    void everyGarmentHasThreeSizeSpecs() {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
        List<Long> ids = garments.stream().map(Garment::getId).toList();
        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(ids);

        assertThat(specs).hasSize(18);
        assertThat(specs).allSatisfy(spec -> {
            assertThat(spec.getSize()).isIn("S", "M", "L");
            assertThat(spec.getMeasurements()).isNotEmpty();
            assertThat(spec.getTargetEase()).isNotEmpty();
            // 실측 치수가 있는데 목표 여유가 없으면 그 부위는 판정할 수 없습니다.
            assertThat(spec.getTargetEase().keySet())
                    .containsExactlyInAnyOrderElementsOf(spec.getMeasurements().keySet());
        });
    }

    @Test
    @DisplayName("핏 3종 × 부위 4개의 허용 범위가 모두 등록되어 있다")
    void everyFitHasToleranceForAllParts() {
        assertThat(fitToleranceRepository.findAll()).hasSize(12);

        for (String fit : List.of("슬림", "레귤러", "오버핏")) {
            assertThat(fitToleranceRepository.findByFit(fit))
                    .extracting(FitTolerance::getPart)
                    .containsExactlyInAnyOrder(
                            "shoulder_width", "chest_circ", "waist_circ", "hip_circ");
        }
    }

    @Test
    @DisplayName("의류의 핏에 해당하는 허용 범위를 부위별로 찾을 수 있다")
    void toleranceIsResolvableForEveryGarmentPart() {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(
                garments.stream().map(Garment::getId).toList());

        assertThat(specs).allSatisfy(spec -> {
            List<String> parts = fitToleranceRepository.findByFit(spec.getGarment().getFit())
                    .stream().map(FitTolerance::getPart).toList();
            assertThat(parts).containsAll(spec.getMeasurements().keySet());
        });
    }
}
