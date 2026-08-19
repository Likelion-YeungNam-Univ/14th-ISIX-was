package com.closr.domain.fitting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

import com.closr.domain.avatar.BodyGridMatcher;
import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.garment.GarmentAssetResolver;
import com.closr.domain.garment.UnavailableReason;
import com.closr.domain.garment.entity.FitTolerance;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.FitToleranceRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 피팅 판정 로직 테스트.
 *
 * <p>시뮬레이션 파트의 {@code fit_judge.py} 와 같은 결과가 나와야 합니다.
 * 아래 수치는 실제 시드 데이터(tshirt_basic)와 fit_judge.py 의 기본 사용자를 그대로 씁니다.
 */
class FittingServiceTest {

    private static final String ASSETS_BASE = "https://assets.example/garments/v1";

    private AvatarRepository avatarRepository;
    private GarmentRepository garmentRepository;
    private GarmentSizeSpecRepository garmentSizeSpecRepository;
    private FitToleranceRepository fitToleranceRepository;
    private FittingRecordService fittingRecordService;
    private BodyGridMatcher bodyGridMatcher;
    private FittingService fittingService;

    private Session session;
    private Garment garment;

    @BeforeEach
    void setUp() {
        avatarRepository = Mockito.mock(AvatarRepository.class);
        garmentRepository = Mockito.mock(GarmentRepository.class);
        garmentSizeSpecRepository = Mockito.mock(GarmentSizeSpecRepository.class);
        fitToleranceRepository = Mockito.mock(FitToleranceRepository.class);
        fittingRecordService = Mockito.mock(FittingRecordService.class);

        // 착용 불가 목록은 목이 아니라 실제 missing_combos.json 을 읽습니다. 주소 조합이
        // 파일명 규칙과 어긋나면 R2 에서 404 가 나는데, 목으로 덮으면 그걸 못 잡습니다.
        // 미리보기 없는 조합의 응답을 확인하려면 목록에 값이 있어야 합니다. 운영
        // 파일은 비어 있을 수 있어 픽스처를 씁니다(missing_combos_fixture.json).
        GarmentAssetResolver assetResolver = new GarmentAssetResolver(
                new ObjectMapper(), "missing_combos_fixture.json", ASSETS_BASE);
        assetResolver.load();

        // 구간 배정은 BodyGridMatcherTest 에서 이미 검증합니다. 여기서는 어떤 구간이
        // 나왔을 때 주소가 어떻게 조합되는지만 보므로 값을 직접 정해줍니다.
        bodyGridMatcher = Mockito.mock(BodyGridMatcher.class);
        given(bodyGridMatcher.assign(anyDouble(), anyDouble())).willReturn("H1B1");

        fittingService = new FittingService(avatarRepository, garmentRepository,
                garmentSizeSpecRepository, fitToleranceRepository, fittingRecordService,
                bodyGridMatcher, assetResolver);

        session = Mockito.mock(Session.class);
        given(session.getId()).willReturn(1L);

        garment = Garment.builder()
                .design("tshirt_basic").name("베이직 티셔츠").category("top").fit("레귤러")
                .build();

        given(garmentRepository.findById(1L)).willReturn(Optional.of(garment));
        given(fitToleranceRepository.findByFit("레귤러")).willReturn(List.of(
                FitTolerance.builder().fit("레귤러").part("chest_circ").devMin(-4.0).devMax(6.0).build(),
                FitTolerance.builder().fit("레귤러").part("shoulder_width").devMin(-1.0).devMax(2.0).build()
        ));
        given(garmentSizeSpecRepository.findByGarmentIdIn(any())).willReturn(List.of(
                spec("s", 99.0, 35.0, 14.0, -2.0),
                spec("m", 108.0, 37.0, 14.0, -2.0),
                spec("l", 114.0, 38.7, 14.0, -1.8)
        ));
    }

    private GarmentSizeSpec spec(String size, double chest, double shoulder,
                                 double chestTarget, double shoulderTarget) {
        return GarmentSizeSpec.builder()
                .garment(garment)
                .size(size)
                .measurements(Map.of("chest_circ", chest, "shoulder_width", shoulder))
                .targetEase(Map.of("chest_circ", chestTarget, "shoulder_width", shoulderTarget))
                .build();
    }

    private void givenAvatar(Map<String, Double> measurements) {
        givenAvatar(measurements, 162);
    }

    private void givenAvatar(Map<String, Double> measurements, Integer height) {
        Avatar avatar = Avatar.builder()
                .session(session).status("done").height(height).measurements(measurements).build();
        given(avatarRepository.findById(1L)).willReturn(Optional.of(avatar));
    }

    private ResponseSizeDetailDto sizeOf(ResponseFittingDto response, String size) {
        return switch (size) {
            case "s" -> response.sizes().s();
            case "m" -> response.sizes().m();
            default -> response.sizes().l();
        };
    }

    @Test
    @DisplayName("fit_judge.py 기본 사용자에게 같은 사이즈를 추천한다")
    void recommendsSameSizeAsReferenceImplementation() {
        // fit_judge.py 의 기본 사용자: bust 88, shoulder 38 → tshirt_basic 은 S 추천
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        assertThat(response.recommendedSize()).isEqualTo("s");
        assertThat(sizeOf(response, "s").recommended()).isTrue();
        assertThat(sizeOf(response, "m").recommended()).isFalse();
    }

    @Test
    @DisplayName("여유량과 편차를 계산해 부위별로 판정한다")
    void computesEaseAndDeviationPerPart() {
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        ResponseFitPartDto chest = sizeOf(response, "s").parts().stream()
                .filter(part -> part.part().equals("chest_circ")).findFirst().orElseThrow();

        // 의류 99 - 아바타 88 = 여유 11, 목표 14 이므로 편차 -3 → 허용범위(-4~6) 안
        assertThat(chest.actualEase()).isEqualTo(11.0);
        assertThat(chest.refEase()).isEqualTo(14.0);
        assertThat(chest.deviation()).isEqualTo(-3.0);
        assertThat(chest.verdict()).isEqualTo("적정");
        assertThat(chest.color()).isEqualTo("green");
    }

    @Test
    @DisplayName("허용 범위를 넘으면 여유 있음으로 판정하되 착용 가능으로 본다")
    void looseIsStillWearable() {
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);
        ResponseSizeDetailDto large = sizeOf(response, "l");

        // L 의 가슴 편차는 +12 로 허용범위(-4~6)를 넘습니다
        assertThat(large.parts()).anySatisfy(part -> {
            assertThat(part.part()).isEqualTo("chest_circ");
            assertThat(part.verdict()).isEqualTo("여유 있음");
        });
        assertThat(large.wearable()).isTrue();
    }

    @Test
    @DisplayName("꽉 끼는 부위가 있으면 착용 불가로 표시한다")
    void tightPartMakesSizeUnwearable() {
        // 가슴이 매우 큰 사용자 — 모든 사이즈가 꽉 낍니다
        givenAvatar(Map.of("chest_circ", 130.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        assertThat(sizeOf(response, "s").wearable()).isFalse();
        assertThat(sizeOf(response, "l").wearable()).isFalse();
        // 전부 불가여도 가장 덜 심한 것을 추천합니다
        assertThat(response.recommendedSize()).isEqualTo("l");
        assertThat(response.recommendationReason()).contains("꽉");
    }

    @Test
    @DisplayName("아바타 계측이 없으면 아직 준비되지 않은 것으로 처리한다")
    void rejectsAvatarWithoutMeasurements() {
        givenAvatar(null);

        assertThatThrownBy(() -> fittingService.getFitting(session, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("아바타 생성이 아직");
    }

    @Test
    @DisplayName("다른 세션의 아바타는 조회할 수 없다")
    void rejectsOtherSessionsAvatar() {
        Session other = Mockito.mock(Session.class);
        given(other.getId()).willReturn(2L);
        Avatar avatar = Avatar.builder()
                .session(other).status("done")
                .measurements(Map.of("chest_circ", 88.0)).build();
        given(avatarRepository.findById(1L)).willReturn(Optional.of(avatar));

        assertThatThrownBy(() -> fittingService.getFitting(session, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한");
    }

    @Test
    @DisplayName("체형 구간을 붙여 GLB · 여유량 주소를 조합한다")
    void composesAssetUrlsWithBodyBucket() {
        // 키 162 · 가슴 88 → H1B1
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);
        ResponseSizeDetailDto medium = sizeOf(response, "m");

        assertThat(medium.glbUrl())
                .isEqualTo(ASSETS_BASE + "/tshirt_basic_m__H1B1.glb");
        // .json 이 아니라 _ease.json 입니다. R2 에 올라간 파일명이 그렇습니다.
        assertThat(medium.easeUrl())
                .isEqualTo(ASSETS_BASE + "/tshirt_basic_m__H1B1_ease.json");
        assertThat(medium.unavailableReason()).isNull();
    }

    @Test
    @DisplayName("사이즈가 대문자로 저장돼 있어도 주소는 소문자로 조합한다")
    void lowercasesSizeInAssetUrl() {
        // R2 는 키 대소문자를 구분해서 _M__ 로 조합하면 404 가 납니다.
        given(garmentSizeSpecRepository.findByGarmentIdIn(any())).willReturn(List.of(
                spec("S", 99.0, 35.0, 14.0, -2.0),
                spec("M", 108.0, 37.0, 14.0, -2.0),
                spec("L", 114.0, 38.7, 14.0, -1.8)
        ));
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        assertThat(response.recommendedSize()).isEqualTo("s");
        assertThat(response.sizes().m()).isNotNull();
        assertThat(sizeOf(response, "m").glbUrl())
                .isEqualTo(ASSETS_BASE + "/tshirt_basic_m__H1B1.glb");
    }

    @Test
    @DisplayName("착용 불가 조합은 주소 없이 사유만 내려주고 판정은 그대로 제공한다")
    void marksImpossibleCombinationUnavailable() {
        // shirt_slim_s__H2B2 는 픽스처 목록에 있는 조합입니다.
        // 미리보기를 만들지 못한 조합입니다. 착용 가능 여부는 판정이 따로 정합니다.
        Garment slim = Garment.builder()
                .design("shirt_slim").name("슬림 셔츠").category("top").fit("슬림").build();
        given(garmentRepository.findById(2L)).willReturn(Optional.of(slim));
        given(fitToleranceRepository.findByFit("슬림")).willReturn(List.of(
                FitTolerance.builder().fit("슬림").part("chest_circ").devMin(-2.0).devMax(3.0).build()
        ));
        given(garmentSizeSpecRepository.findByGarmentIdIn(any())).willReturn(List.of(
                slimSpec(slim, "s", 93.0), slimSpec(slim, "m", 102.0), slimSpec(slim, "l", 108.0)
        ));

        given(bodyGridMatcher.assign(anyDouble(), anyDouble())).willReturn("H2B2");
        givenAvatar(Map.of("chest_circ", 95.0), 170);

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 2L);
        ResponseSizeDetailDto small = sizeOf(response, "s");

        assertThat(small.unavailableReason()).isEqualTo(UnavailableReason.TOO_SMALL);
        assertThat(small.glbUrl()).isNull();
        assertThat(small.easeUrl()).isNull();
        // 미리보기만 없고 판정은 정상입니다. "안 맞습니다" 가 정답이므로 에러가 아닙니다.
        assertThat(small.parts()).isNotEmpty();

        // 같은 체형의 m 은 파일이 있어 정상입니다.
        ResponseSizeDetailDto medium = sizeOf(response, "m");
        assertThat(medium.unavailableReason()).isNull();
        assertThat(medium.glbUrl()).isEqualTo(ASSETS_BASE + "/shirt_slim_m__H2B2.glb");
    }

    @Test
    @DisplayName("키가 없어 체형 구간을 못 정하면 판정만 내려준다")
    void returnsJudgementWithoutPreviewWhenBucketUnknown() {
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0), null);

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);
        ResponseSizeDetailDto small = sizeOf(response, "s");

        assertThat(small.unavailableReason()).isEqualTo(UnavailableReason.SIMULATION_FAILED);
        assertThat(small.glbUrl()).isNull();
        // 판정은 실측으로 하므로 구간이 없어도 나옵니다.
        assertThat(small.parts()).isNotEmpty();
        assertThat(response.recommendedSize()).isEqualTo("s");
    }

    private GarmentSizeSpec slimSpec(Garment slim, String size, double chest) {
        return GarmentSizeSpec.builder()
                .garment(slim)
                .size(size)
                .measurements(Map.of("chest_circ", chest))
                .targetEase(Map.of("chest_circ", 8.0))
                .build();
    }
}
