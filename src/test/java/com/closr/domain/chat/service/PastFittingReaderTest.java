package com.closr.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.user.entity.Session;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 지난 피팅 읽기 테스트.
 *
 * <p>여기서 지키려는 것이 셋입니다. <b>같은 옷이 중복되지 않는지</b>,
 * <b>지금 보는 옷이 빠지는지</b>, <b>꽉 낀 부위를 그때 저장한 값에서 읽는지</b>입니다.
 */
class PastFittingReaderTest {

    private FittingRecordRepository repository;
    private PastFittingReader reader;
    private Session session;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FittingRecordRepository.class);
        reader = new PastFittingReader(repository);
        session = Mockito.mock(Session.class);
    }

    /** id 는 DB 가 넣는 값이라 테스트에서는 리플렉션으로 채웁니다. */
    private Garment garment(Long id, String design) {
        Garment garment = Garment.builder()
                .design(design).name(design).category("top").fit("슬림").build();
        setId(garment, id);
        return garment;
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private FittingRecord record(Garment garment, String recommended, boolean wearable,
                                 Map<String, Object> result) {
        FittingRecord record = FittingRecord.builder()
                .session(session).garment(garment)
                .recommendedSize(recommended).wearable(wearable).result(result).build();
        return record;
    }

    /** 그때 내려준 응답 전문의 모양입니다. */
    private Map<String, Object> resultWith(String size, String... tightParts) {
        List<Map<String, Object>> parts = java.util.Arrays.stream(tightParts)
                .map(part -> Map.<String, Object>of("part", part, "verdict", "꽉 낌"))
                .toList();
        return Map.of("sizes", Map.of(size, Map.of("parts", parts)));
    }

    @Test
    @DisplayName("최신순으로 가져오되 지금 보고 있는 옷은 제외한다")
    void excludesCurrentGarment() {
        // 자기 자신과 비교하면 "이 옷은 이 옷과 같습니다" 가 됩니다.
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(garment(1L, "shirt_slim"), "m", true, resultWith("m")),
                record(garment(2L, "pants_slacks"), "m", true, resultWith("m"))));

        List<Map<String, Object>> past = reader.read(session, 1L);

        assertThat(past).hasSize(1);
        assertThat(past.get(0)).containsEntry("garment_id", "pants_slacks");
    }

    @Test
    @DisplayName("같은 옷을 여러 번 봤으면 최신 것만 남긴다")
    void keepsOnlyLatestPerGarment() {
        // 안 그러면 같은 셔츠가 다섯 번 들어가고 비교할 다른 옷이 밀려 나갑니다.
        Garment slim = garment(1L, "shirt_slim");
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(slim, "l", true, resultWith("l")),
                record(slim, "m", false, resultWith("m", "shoulder_width")),
                record(garment(2L, "skirt_pencil"), "m", true, resultWith("m"))));

        List<Map<String, Object>> past = reader.read(session, null);

        assertThat(past).hasSize(2);
        // 최신(첫 번째)이 남습니다.
        assertThat(past.get(0)).containsEntry("size", "l");
        assertThat(past.get(0)).containsEntry("tight_parts", List.of());
    }

    @Test
    @DisplayName("꽉 낀 부위를 그때 저장한 결과에서 읽는다")
    void readsTightPartsFromStoredResult() {
        // 판정 기준이 나중에 바뀌어도 사용자가 그때 본 값이 나와야 합니다.
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(garment(1L, "shirt_slim"), "s", false,
                        resultWith("s", "shoulder_width", "chest_circ"))));

        List<Map<String, Object>> past = reader.read(session, null);

        assertThat(past.get(0)).containsEntry("tight_parts",
                List.of("shoulder_width", "chest_circ"));
        assertThat(past.get(0)).containsEntry("wearable", false);
    }

    @Test
    @DisplayName("다섯 건까지만 가져온다")
    void limitsToFive() {
        // 답변이 2~3문장이라 더 넣으면 토큰만 늘고 모델이 엉뚱한 것을 고릅니다.
        List<FittingRecord> many = new java.util.ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            many.add(record(garment(i, "design" + i), "m", true, resultWith("m")));
        }
        given(repository.findAllBySessionWithGarment(session)).willReturn(many);

        assertThat(reader.read(session, null)).hasSize(5);
    }

    @Test
    @DisplayName("기록이 없으면 빈 배열을 준다")
    void returnsEmptyWithoutRecords() {
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of());

        assertThat(reader.read(session, 1L)).isEmpty();
    }

    @Test
    @DisplayName("저장된 결과 형태가 달라도 비교 발화만 빠지고 나머지는 채운다")
    void survivesUnexpectedResultShape() {
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(garment(1L, "shirt_slim"), "m", true, Map.of("sizes", "형식이 다름")),
                record(garment(2L, "shirt_over"), "m", true, null)));

        List<Map<String, Object>> past = reader.read(session, null);

        assertThat(past).hasSize(2);
        assertThat(past).allSatisfy(entry ->
                assertThat(entry).containsEntry("tight_parts", List.of()));
        assertThat(past.get(0)).containsEntry("garment_id", "shirt_slim");
    }

    @Test
    @DisplayName("대문자로 저장된 옛 기록도 소문자로 내려준다")
    void normalizesStoredSize() {
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(garment(1L, "shirt_slim"), "M", true, resultWith("m"))));

        List<Map<String, Object>> past = reader.read(session, null);

        assertThat(past.get(0)).containsEntry("size", "m");
        assertThat(past.get(0)).containsEntry("recommended_size", "m");
    }
}
