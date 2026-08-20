package com.closr.domain.avatar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 체형별 스타일링 문구 테스트.
 *
 * <p>편집 문구라 내용을 검증할 수는 없습니다. 대신 <b>빠진 체형이 없는지</b>와
 * <b>없는 품목을 권하지 않는지</b>를 지킵니다. 뒤쪽이 실제 위험입니다 — 카탈로그에
 * 없는 옷을 권하면 사용자가 찾아도 나오지 않습니다.
 */
class BodyTypeStylingTest {

    /** AI 파트 {@code bodytype.py} 의 LABELS 키와 같아야 합니다. */
    private static final List<String> BODY_TYPES = List.of(
            "hourglass", "triangle", "inverted_triangle", "rectangle", "round");

    /** 카탈로그에 실제로 있는 품목입니다. 문구가 언급할 수 있는 전부입니다. */
    private static final Set<String> CATALOG = Set.of(
            "티셔츠", "셔츠", "원피스", "슬랙스", "스커트");

    /** 없는 품목을 권하면 막다른 길이 됩니다. 과거 프론트 임시값에 있던 것들입니다. */
    private static final Set<String> NOT_IN_CATALOG = Set.of(
            "A라인", "랩 드레스", "재킷", "청바지", "코트", "니트");

    @ParameterizedTest
    @ValueSource(strings = {"hourglass", "triangle", "inverted_triangle", "rectangle", "round"})
    @DisplayName("체형 5종에 모두 문구가 있다")
    void everyBodyTypeHasTips(String bodyType) {
        assertThat(BodyTypeStyling.of(bodyType))
                .as(bodyType)
                .hasSize(3)
                .allSatisfy(tip -> assertThat(tip).isNotBlank());
    }

    @Test
    @DisplayName("카탈로그에 없는 품목을 권하지 않는다")
    void mentionsOnlyGarmentsWeHave() {
        for (String bodyType : BODY_TYPES) {
            for (String tip : BodyTypeStyling.of(bodyType)) {
                assertThat(NOT_IN_CATALOG)
                        .as("%s — \"%s\"", bodyType, tip)
                        .noneSatisfy(missing -> assertThat(tip).contains(missing));
            }
        }
    }

    @Test
    @DisplayName("체형을 못 정했으면 빈 목록이다")
    void unknownBodyTypeIsEmpty() {
        // 계측이 실패하면 bodyType 이 null 로 옵니다. 예외를 던지면 아바타
        // 자체를 못 열게 되므로 이 영역만 비워야 합니다.
        assertThat(BodyTypeStyling.of(null)).isEmpty();
        assertThat(BodyTypeStyling.of("pear")).isEmpty();
        assertThat(BodyTypeStyling.of("")).isEmpty();
    }

    @Test
    @DisplayName("문구가 퍼센트 같은 수치 형태를 쓰지 않는다")
    void tipsCarryNoFakeNumbers() {
        // 편집 문구에 숫자가 섞이면 계산된 값으로 보입니다. 그러면 실제 판정
        // 수치까지 같은 의심을 받습니다.
        for (String bodyType : BODY_TYPES) {
            assertThat(BodyTypeStyling.of(bodyType))
                    .as(bodyType)
                    .allSatisfy(tip -> assertThat(tip).doesNotContain("%"));
        }
    }
}
