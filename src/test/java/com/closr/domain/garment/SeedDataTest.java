package com.closr.domain.garment;

import static org.assertj.core.api.Assertions.assertThat;

import com.closr.domain.garment.entity.FitTolerance;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.FitToleranceRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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
    @DisplayName("의류마다 s · m · l 스펙이 있고 실측 치수와 목표 여유가 같은 부위를 담는다")
    void everyGarmentHasThreeSizeSpecs() {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
        List<Long> ids = garments.stream().map(Garment::getId).toList();
        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(ids);

        assertThat(specs).hasSize(18);
        assertThat(specs).allSatisfy(spec -> {
            // 소문자입니다. R2 키가 {design}_{size}__{bucket}.glb 이고 R2 는
            // 대소문자를 구분하므로, 대문자가 섞이면 조합한 주소가 전부 404 가 됩니다.
            assertThat(spec.getSize()).isIn("s", "m", "l");
            assertThat(spec.getMeasurements()).isNotEmpty();
            assertThat(spec.getTargetEase()).isNotEmpty();
            // 실측 치수가 있는데 목표 여유가 없으면 그 부위는 판정할 수 없습니다.
            assertThat(spec.getTargetEase().keySet())
                    .containsExactlyInAnyOrderElementsOf(spec.getMeasurements().keySet());
        });
    }

    @Test
    @DisplayName("시드가 돌고 나면 대문자 사이즈가 남지 않는다")
    void noUppercaseSizeSurvivesSeeding() {
        // 대문자가 남아 있으면 R2 파일명이 _M__ 로 조합돼 404 가 납니다.
        //
        // 더 급한 문제는 다음 기동입니다. 소문자 행이 이미 있는데 대문자 행도 남아
        // 있으면 data.sql 의 lower() 가 uk_garment_size 를 위반해 서버가 아예 뜨지
        // 않습니다. 그래서 이 테스트가 통과한다는 것은 "다음 기동이 가능하다" 는
        // 뜻이기도 합니다.
        assertThat(garmentSizeSpecRepository.findAll())
                .isNotEmpty()
                .allSatisfy(spec ->
                        assertThat(spec.getSize()).isEqualTo(spec.getSize().toLowerCase(Locale.ROOT)));
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
    @DisplayName("어깨 목표 여유가 2026-08-07 재보정 값이다")
    void shoulderTargetEaseIsRecalibrated() {
        // 재보정 전 값(-1.4 ~ -2.0)이 남아 있으면 어깨 편차가 약 7cm 여유 있는 쪽으로
        // 밀려서, 안 맞는 사이즈를 적정으로 판정합니다. 값이 조용히 되돌아가는 것을
        // 막기 위해 실제 수치를 고정합니다. 출처는 의류 파트의
        // garment/config/garment_target_ease.csv 입니다.
        Map<String, Double> expected = Map.ofEntries(
                Map.entry("tshirt_basic s", -7.9),
                Map.entry("tshirt_basic m", -9.0),
                Map.entry("tshirt_basic l", -8.9),
                Map.entry("shirt_slim s", -7.9),
                Map.entry("shirt_slim m", -9.0),
                Map.entry("shirt_slim l", -9.0),
                Map.entry("shirt_over s", -7.9),
                Map.entry("shirt_over m", -9.0),
                Map.entry("shirt_over l", -8.5),
                Map.entry("dress_basic s", -7.9),
                Map.entry("dress_basic m", -9.0),
                Map.entry("dress_basic l", -8.9));

        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(
                garments.stream().map(Garment::getId).toList());

        for (GarmentSizeSpec spec : specs) {
            Double shoulder = spec.getTargetEase().get("shoulder_width");
            if (shoulder == null) {
                continue;   // 하의는 어깨를 판정하지 않습니다.
            }
            String key = spec.getGarment().getDesign() + " " + spec.getSize();
            assertThat(shoulder).as(key).isEqualTo(expected.get(key));
        }
    }

    @Test
    @DisplayName("목표 여유가 그 사이즈의 기준 체형에서 편차 0 이 되는 값이다")
    void targetEaseIsGridBaselined() throws Exception {
        // 판정은 body_grid.json 좌표계에서 정의돼 있습니다. 목표 여유가 다른
        // 기준(마네킹 상수 등)에서 오면 그만큼 통째로 치우칩니다. 실제로 2026-08-17
        // 전까지 허리가 그랬고, S 목표 체형을 정확히 가진 사람이 S 바지를 입으면
        // 편차 +8.3 으로 "여유 있음" 판정을 받았습니다. 자기 사이즈인데요.
        //
        // 그래서 값을 하나씩 박는 대신 관계를 검증합니다.
        //
        //     목표 여유 = 의류 치수 − 기준 체형 치수
        //
        // 이러면 두 가지가 같이 잡힙니다. 목표 여유가 딴 데서 오는 경우와,
        // 의류 치수만 바뀌고 목표 여유를 다시 안 뽑은 경우입니다. 뒤쪽은 편차
        // 계산에서 의류 항이 소거되지 않아 그 차이가 판정에 그대로 남습니다.
        Map<String, Map<String, Double>> grid = loadGridMeasurements();

        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(
                garments.stream().map(Garment::getId).toList());

        assertThat(specs).isNotEmpty();
        // 부위를 하나도 못 찾으면 이 테스트는 아무것도 검증하지 않은 채 통과합니다.
        // 실제로 센 개수를 남겨 그 경우를 막습니다. 의류 6종 × 부위 합계입니다.
        int compared = 0;

        for (GarmentSizeSpec spec : specs) {
            String size = spec.getSize().toLowerCase(Locale.ROOT);
            Map<String, Double> body = grid.get(TARGET_BUCKET.get(size));
            assertThat(body).as("사이즈 %s 의 기준 구간", size).isNotNull();

            for (Map.Entry<String, Double> entry : spec.getMeasurements().entrySet()) {
                String part = entry.getKey();
                Double garmentCm = entry.getValue();
                Double targetEase = spec.getTargetEase().get(part);
                Double bodyCm = body.get(part);
                if (targetEase == null || bodyCm == null) {
                    continue;   // 격자에 없는 부위는 판정 대상이 아닙니다.
                }
                String key = spec.getGarment().getDesign() + " " + size + " " + part;
                assertThat(targetEase)
                        .as("%s — 의류 %.1f, 기준 체형 %.1f", key, garmentCm, bodyCm)
                        .isEqualTo(round1(garmentCm - bodyCm));
                compared++;
            }
        }

        assertThat(compared).as("검증한 부위 수").isEqualTo(36);
    }

    /**
     * 사이즈별 기준 체형 구간.
     *
     * <p>의류 파트 {@code make_spec.py} 가 목표 여유를 뽑을 때 쓰는 몸입니다.
     * 어깨 목표 여유를 역산해도 같은 구간이 나옵니다 — 예를 들어
     * {@code tshirt_basic m} 의 −9.0 은 37.0 − 46.0(H1B2) 입니다.
     *
     * <p>이 대응이 바뀌면 목표 여유 전체를 다시 뽑아야 합니다.
     */
    private static final Map<String, String> TARGET_BUCKET =
            Map.of("s", "H1B0", "m", "H1B2", "l", "H1B3");

    /** body_grid.json 의 구간별 실측 치수. 판정 좌표계의 원점입니다. */
    private Map<String, Map<String, Double>> loadGridMeasurements() throws Exception {
        try (var in = new ClassPathResource("body_grid.json").getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(in);
            Map<String, Map<String, Double>> grid = new LinkedHashMap<>();
            for (JsonNode bucket : root.path("buckets")) {
                Map<String, Double> body = new LinkedHashMap<>();
                bucket.path("measurements_cm").properties()
                        .forEach(entry -> body.put(entry.getKey(), entry.getValue().asDouble()));
                grid.put(bucket.path("id").asText(), body);
            }
            return grid;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    @Test
    @DisplayName("어깨 허용 범위가 의류 파트 CSV 와 일치한다")
    void shoulderToleranceMatchesGarmentCsv() {
        // garment/config/garment_tolerance.csv 의 값입니다. 시드가 -1 로 들어가 있어
        // 편차 -1.5 인 어깨가 CSV 기준으로는 적정인데 꽉 낌으로 판정되던 문제가 있었습니다.
        Map<String, double[]> expected = Map.of(
                "슬림", new double[] {-2.0, 2.0},
                "레귤러", new double[] {-2.0, 3.0},
                "오버핏", new double[] {-2.0, 5.0});

        expected.forEach((fit, range) -> {
            FitTolerance tolerance = fitToleranceRepository.findByFit(fit).stream()
                    .filter(t -> "shoulder_width".equals(t.getPart()))
                    .findFirst()
                    .orElseThrow();
            assertThat(tolerance.getDevMin()).as(fit + " dev_min").isEqualTo(range[0]);
            assertThat(tolerance.getDevMax()).as(fit + " dev_max").isEqualTo(range[1]);
        });
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
