package com.closr.domain.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 체형 12구간 배정 테스트.
 *
 * <p>격자 정의는 아바타 · 의류 · 백엔드가 공유하는 파일이라, 값이 바뀌면
 * 여기서 먼저 깨져야 합니다.
 */
class BodyGridMatcherTest {

    private BodyGridMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new BodyGridMatcher(new ObjectMapper(), BodyGridMatcher.DEFAULT_RESOURCE);
        matcher.load();
    }

    @Test
    @DisplayName("원본 파일에 적힌 예시와 같은 구간을 반환한다")
    void matchesExampleInGridFile() {
        // body_grid.json 의 assignment.example: "키 160, 가슴 94 -> H1B2"
        assertThat(matcher.assign(160, 94)).isEqualTo("H1B2");
    }

    @ParameterizedTest(name = "키 {0}, 가슴 {1} → {2}")
    @CsvSource({
            "154, 83.0,   H0B0",
            "175, 110.0,  H2B3",
            "165, 87.2,   H1B1"
    })
    @DisplayName("키와 가슴둘레로 구간을 정한다")
    void assignsBucket(double height, double chest, String expected) {
        assertThat(matcher.assign(height, chest)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "키 {0} → {1}")
    @CsvSource({"157.9, H0B0", "158.0, H1B0", "165.9, H1B0", "166.0, H2B0"})
    @DisplayName("키 경계값은 위쪽 구간에 포함된다")
    void heightBoundaryBelongsToUpperBucket(double height, String expected) {
        assertThat(matcher.assign(height, 80)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "가슴 {0} → {1}")
    @CsvSource({"85.4, H0B0", "85.5, H0B1", "90.9, H0B1", "91.0, H0B2", "97.5, H0B3"})
    @DisplayName("가슴둘레 경계값은 위쪽 구간에 포함된다")
    void chestBoundaryBelongsToUpperBucket(double chest, String expected) {
        assertThat(matcher.assign(150, chest)).isEqualTo(expected);
    }

    @Test
    @DisplayName("어떤 입력이든 정의된 12구간 안으로 떨어진다")
    void everyInputFallsIntoDefinedBucket() {
        Set<String> seen = new LinkedHashSet<>();

        for (int height = 130; height <= 200; height++) {
            for (double chest = 60; chest <= 130; chest += 0.5) {
                seen.add(matcher.assign(height, chest));
            }
        }

        assertThat(seen).hasSize(12);
        assertThat(seen).allSatisfy(id -> assertThat(id).matches("H[0-2]B[0-3]"));
    }

    @Test
    @DisplayName("격자 파일이 없으면 기동에 실패한다")
    void failsWhenGridFileMissing() {
        BodyGridMatcher broken = new BodyGridMatcher(new ObjectMapper(), "no_such_grid.json");

        assertThatThrownBy(broken::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("읽지 못했습니다");
    }

    @Test
    @DisplayName("schema_version 이 다르면 기동에 실패한다")
    void failsWhenSchemaVersionDiffers() {
        BodyGridMatcher broken =
                new BodyGridMatcher(new ObjectMapper(), "body_grid_wrong_version.json");

        assertThatThrownBy(broken::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema_version");
    }

    @Test
    @DisplayName("경계값으로 만들 구간 수와 정의된 구간 수가 다르면 기동에 실패한다")
    void failsWhenBucketCountMismatches() {
        BodyGridMatcher broken =
                new BodyGridMatcher(new ObjectMapper(), "body_grid_bucket_mismatch.json");

        assertThatThrownBy(broken::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("구간");
    }
}
