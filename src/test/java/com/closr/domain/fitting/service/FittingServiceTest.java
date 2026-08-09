package com.closr.domain.fitting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.garment.entity.FitTolerance;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.FitToleranceRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
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

    private AvatarRepository avatarRepository;
    private GarmentRepository garmentRepository;
    private GarmentSizeSpecRepository garmentSizeSpecRepository;
    private FitToleranceRepository fitToleranceRepository;
    private FittingRecordService fittingRecordService;
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

        fittingService = new FittingService(avatarRepository, garmentRepository,
                garmentSizeSpecRepository, fitToleranceRepository, fittingRecordService);

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
                spec("S", 99.0, 35.0, 14.0, -2.0),
                spec("M", 108.0, 37.0, 14.0, -2.0),
                spec("L", 114.0, 38.7, 14.0, -1.8)
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
        Avatar avatar = Avatar.builder()
                .session(session).status("done").measurements(measurements).build();
        given(avatarRepository.findById(1L)).willReturn(Optional.of(avatar));
    }

    private ResponseSizeDetailDto sizeOf(ResponseFittingDto response, String size) {
        return switch (size) {
            case "S" -> response.sizes().s();
            case "M" -> response.sizes().m();
            default -> response.sizes().l();
        };
    }

    @Test
    @DisplayName("fit_judge.py 기본 사용자에게 같은 사이즈를 추천한다")
    void recommendsSameSizeAsReferenceImplementation() {
        // fit_judge.py 의 기본 사용자: bust 88, shoulder 38 → tshirt_basic 은 S 추천
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        assertThat(response.recommendedSize()).isEqualTo("S");
        assertThat(sizeOf(response, "S").recommended()).isTrue();
        assertThat(sizeOf(response, "M").recommended()).isFalse();
    }

    @Test
    @DisplayName("여유량과 편차를 계산해 부위별로 판정한다")
    void computesEaseAndDeviationPerPart() {
        givenAvatar(Map.of("chest_circ", 88.0, "shoulder_width", 38.0));

        ResponseFittingDto response = fittingService.getFitting(session, 1L, 1L);

        ResponseFitPartDto chest = sizeOf(response, "S").parts().stream()
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
        ResponseSizeDetailDto large = sizeOf(response, "L");

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

        assertThat(sizeOf(response, "S").wearable()).isFalse();
        assertThat(sizeOf(response, "L").wearable()).isFalse();
        // 전부 불가여도 가장 덜 심한 것을 추천합니다
        assertThat(response.recommendedSize()).isEqualTo("L");
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
}
