package com.closr.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.fitting.dto.ResponseSizeOptionsDto;
import com.closr.domain.fitting.service.FittingService;
import com.closr.domain.garment.UnavailableReason;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.user.entity.Session;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * fit_context 조립 테스트.
 *
 * <p>여기서 지키려는 것이 셋입니다. <b>AI 표기로 변환되는지</b>,
 * <b>판정이 화면과 같은 값인지</b>, <b>피팅 기록을 남기지 않는지</b>입니다.
 */
class FitContextAssemblerTest {

    private FittingService fittingService;
    private PastFittingReader pastFittingReader;
    private FitContextAssembler assembler;
    private Session session;
    private Avatar avatar;
    private Garment garment;

    @BeforeEach
    void setUp() {
        fittingService = Mockito.mock(FittingService.class);
        pastFittingReader = Mockito.mock(PastFittingReader.class);
        given(pastFittingReader.read(Mockito.any(), Mockito.any())).willReturn(List.of());
        assembler = new FitContextAssembler(fittingService, pastFittingReader);

        session = Mockito.mock(Session.class);
        avatar = Mockito.mock(Avatar.class);
        given(avatar.getId()).willReturn(1L);
        given(avatar.getMeasurements()).willReturn(Map.of("shoulder_width", 45.6, "chest_circ", 85.3));
        given(avatar.getWarnings()).willReturn(List.of("팔이 몸통에 붙어 있습니다"));

        garment = Garment.builder()
                .design("shirt_slim").name("슬림 셔츠").category("top").fit("슬림").build();
    }

    private void givenFitting(String recommended, ResponseSizeDetailDto s,
                              ResponseSizeDetailDto m, ResponseSizeDetailDto l) {
        ResponseFittingDto response = new ResponseFittingDto(
                2L, new ResponseSizeOptionsDto(s, m, l), recommended, "사유");
        given(fittingService.evaluate(session, 1L, 2L))
                .willReturn(new FittingService.Evaluation(avatar, garment, response, true));
    }

    private ResponseSizeDetailDto detail(UnavailableReason reason, ResponseFitPartDto... parts) {
        return new ResponseSizeDetailDto("glb", "ease", reason, List.of(parts),
                0.0, 1.0, reason == null, false);
    }

    @Test
    @DisplayName("의류를 고르지 않으면 치수와 경고만 보낸다")
    void sendsOnlyMeasurementsWithoutGarment() {
        Map<String, Object> context = assembler.assemble(session, avatar, null, null);

        assertThat(context).containsOnlyKeys(
                "measurements", "warnings", "profile", "past_fittings");
        assertThat(context.get("warnings")).isEqualTo(List.of("팔이 몸통에 붙어 있습니다"));
        // 자리를 비워 둬야 AI 가 항상 같은 형태를 받습니다.
        assertThat(context.get("profile")).isNull();
        assertThat(context.get("past_fittings")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("판정을 AI 표기로 바꿔 보낸다")
    void convertsToAiFieldNames() {
        givenFitting("m",
                detail(null, new ResponseFitPartDto("shoulder_width", -10.5, -7.9, -2.6, "꽉 낌", "red")),
                detail(null), detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "s");

        assertThat(context.get("garment_id")).isEqualTo("shirt_slim");
        assertThat(context.get("fit")).isEqualTo("슬림");
        assertThat(context.get("size")).isEqualTo("s");
        assertThat(context.get("recommended_size")).isEqualTo("m");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> report = (List<Map<String, Object>>) context.get("fit_report");
        assertThat(report).hasSize(1);
        assertThat(report.get(0)).containsExactly(
                Map.entry("part", "shoulder_width"),
                Map.entry("actual_ease", -10.5),
                Map.entry("ref_ease", -7.9),
                Map.entry("deviation", -2.6),
                Map.entry("verdict", "tight"));
    }

    @Test
    @DisplayName("verdict 는 한글 라벨이 아니라 영문 코드로 나간다")
    void sendsVerdictAsEnglishCode() {
        // 한글 라벨을 보내면 프롬프트가 부위 키와 연결하지 못합니다.
        givenFitting("m", detail(null,
                        new ResponseFitPartDto("chest_circ", 6.5, 8.0, -1.5, "적정", "green"),
                        new ResponseFitPartDto("waist_circ", 20.0, 8.0, 12.0, "여유 있음", "blue")),
                detail(null), detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "s");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> report = (List<Map<String, Object>>) context.get("fit_report");
        assertThat(report).extracting(entry -> entry.get("verdict"))
                .containsExactly("good", "loose");
    }

    @Test
    @DisplayName("색은 보내지 않는다")
    void omitsColor() {
        // 챗봇은 "색으로 사이즈를 말하지 않는다" 규칙을 지켜야 합니다.
        givenFitting("m",
                detail(null, new ResponseFitPartDto("chest_circ", 6.5, 8.0, -1.5, "적정", "green")),
                detail(null), detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "s");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> report = (List<Map<String, Object>>) context.get("fit_report");
        assertThat(report.get(0)).doesNotContainKey("color");
    }

    @Test
    @DisplayName("사이즈를 주지 않으면 추천 사이즈의 판정을 보낸다")
    void fallsBackToRecommendedSize() {
        givenFitting("m", detail(null),
                detail(null, new ResponseFitPartDto("chest_circ", 0.0, 8.0, -8.0, "적정", "green")),
                detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, null);

        assertThat(context.get("size")).isEqualTo("m");
    }

    @Test
    @DisplayName("대문자 사이즈로 들어와도 소문자로 조회한다")
    void normalizesSize() {
        givenFitting("m", detail(null), detail(null),
                detail(null, new ResponseFitPartDto("chest_circ", 12.0, 8.0, 4.0, "여유 있음", "blue")));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "L");

        assertThat(context.get("size")).isEqualTo("l");
        assertThat(context.get("fit_report")).asList().hasSize(1);
    }

    @Test
    @DisplayName("착용 불가 조합은 사유를 함께 보낸다")
    void sendsUnavailableReason() {
        givenFitting("m",
                detail(UnavailableReason.TOO_SMALL,
                        new ResponseFitPartDto("chest_circ", -9.0, 8.0, -17.0, "꽉 낌", "red")),
                detail(null), detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "s");

        assertThat(context.get("unavailable_reason")).isEqualTo("TOO_SMALL");
        // 미리보기만 없고 판정은 그대로 보냅니다.
        assertThat(context.get("fit_report")).asList().hasSize(1);
    }

    @Test
    @DisplayName("판정 결과가 없는 사이즈면 치수만 보낸다")
    void skipsFitReportWhenSizeMissing() {
        givenFitting("m", null, detail(null), detail(null));

        Map<String, Object> context = assembler.assemble(session, avatar, 2L, "s");

        assertThat(context).doesNotContainKey("fit_report");
        assertThat(context).containsKey("measurements");
    }

    @Test
    @DisplayName("피팅 기록을 남기지 않는다")
    void doesNotSaveFittingRecord() {
        // getFitting 을 쓰면 대화 한 턴마다 기록이 쌓여 past_fittings 가 오염됩니다.
        givenFitting("m", detail(null), detail(null), detail(null));

        assembler.assemble(session, avatar, 2L, "s");

        Mockito.verify(fittingService).evaluate(session, 1L, 2L);
        Mockito.verify(fittingService, Mockito.never())
                .getFitting(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
