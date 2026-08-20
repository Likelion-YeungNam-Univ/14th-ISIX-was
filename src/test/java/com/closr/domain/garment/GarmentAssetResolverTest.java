package com.closr.domain.garment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 에셋 주소 조합 테스트.
 *
 * <p>실제 {@code missing_combos.json} 을 읽습니다. 파일이 재생성돼 목록이 바뀌면 이
 * 테스트가 먼저 깨지도록 하기 위함입니다.
 *
 * <p>여기서 검증하는 것은 결국 <b>R2 에 올라간 파일명과 글자 단위로 같은지</b>입니다.
 * 한 글자만 달라도 404 가 나고, 서버는 200 을 주기 때문에 프론트 화면에서만 깨집니다.
 */
class GarmentAssetResolverTest {

    private static final String BASE = "https://assets.example/garments/v1";

    /**
     * 목록에 값이 있는 상태를 재현하는 픽스처.
     *
     * <p>운영 파일은 재시뮬 결과에 따라 비어 있을 수 있습니다. 그 파일로만 검증하면
     * 목록이 빌 때 "미리보기 없는 조합을 걸러내는 동작" 자체가 검증에서 사라집니다.
     * 동작은 픽스처로 고정하고, 운영 파일은 형태와 현재 내용만 확인합니다.
     */
    private static final String FIXTURE = "missing_combos_fixture.json";

    private GarmentAssetResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GarmentAssetResolver(
                new ObjectMapper(), GarmentAssetResolver.DEFAULT_RESOURCE, BASE);
        resolver.load();
    }

    @Test
    @DisplayName("GLB 와 여유량 주소를 파일명 규칙대로 조합한다")
    void composesUrls() {
        GarmentAsset asset = resolver.resolve("tshirt_basic", "m", "H1B1");

        assertThat(asset.glbUrl()).isEqualTo(BASE + "/tshirt_basic_m__H1B1.glb");
        // .json 이 아니라 _ease.json 입니다.
        assertThat(asset.easeUrl()).isEqualTo(BASE + "/tshirt_basic_m__H1B1_ease.json");
        assertThat(asset.unavailableReason()).isNull();
    }

    @Test
    @DisplayName("사이즈가 대문자로 들어와도 소문자로 조합한다")
    void lowercasesSize() {
        // R2 는 키 대소문자를 구분해서 _M__ 로 조합하면 404 가 납니다.
        assertThat(resolver.resolve("tshirt_basic", "M", "H1B1").glbUrl())
                .isEqualTo(BASE + "/tshirt_basic_m__H1B1.glb");
    }

    @Test
    @DisplayName("베이스 주소 끝에 슬래시가 있어도 // 를 만들지 않는다")
    void trimsTrailingSlash() {
        GarmentAssetResolver withSlash = new GarmentAssetResolver(
                new ObjectMapper(), GarmentAssetResolver.DEFAULT_RESOURCE, BASE + "/");
        withSlash.load();

        assertThat(withSlash.resolve("tshirt_basic", "m", "H1B1").glbUrl())
                .isEqualTo(BASE + "/tshirt_basic_m__H1B1.glb");
    }

    @Test
    @DisplayName("목록에 오른 조합은 주소 없이 TOO_SMALL 을 돌려준다")
    void marksMissingCombosTooSmall() {
        GarmentAssetResolver withMissing =
                new GarmentAssetResolver(new ObjectMapper(), FIXTURE, BASE);
        withMissing.load();

        GarmentAsset asset = withMissing.resolve("shirt_slim", "s", "H2B2");

        assertThat(asset.unavailableReason()).isEqualTo(UnavailableReason.TOO_SMALL);
        assertThat(asset.glbUrl()).isNull();
        assertThat(asset.easeUrl()).isNull();
    }

    @Test
    @DisplayName("운영 파일에는 현재 빠진 조합이 없다")
    void shippedListIsEmpty() {
        // 2026-08-19 재시뮬에서 shirt_slim 3조합이 전부 미리보기를 만들었습니다.
        // 목록이 다시 채워지면 이 테스트가 먼저 알려줍니다 — 그때는 프론트가
        // unavailableReason 분기를 타는지 함께 확인해야 합니다.
        assertThat(resolver.resolve("shirt_slim", "s", "H2B2").unavailableReason()).isNull();
        assertThat(resolver.resolve("shirt_slim", "s", "H2B3").unavailableReason()).isNull();
        assertThat(resolver.resolve("shirt_slim", "m", "H2B3").unavailableReason()).isNull();
    }

    @Test
    @DisplayName("착용 불가 목록에 없는 인접 조합은 정상으로 본다")
    void neighbouringCombosStayAvailable() {
        // shirt_slim_m__H2B2 는 목록에 없습니다. 디자인 · 사이즈 · 구간 중 하나만
        // 달라도 판단이 뒤집히므로 키를 부분 일치로 비교하면 안 됩니다.
        assertThat(resolver.resolve("shirt_slim", "m", "H2B2").unavailableReason()).isNull();
        assertThat(resolver.resolve("shirt_slim", "l", "H2B3").unavailableReason()).isNull();
        assertThat(resolver.resolve("shirt_over", "s", "H2B2").unavailableReason()).isNull();
    }

    @Test
    @DisplayName("체형 구간이 없으면 SIMULATION_FAILED 로 내려준다")
    void missingBucketIsSimulationFailed() {
        // 조합하면 shirt_slim_s__.glb 가 되어 R2 에서 404 가 납니다.
        for (String bucket : new String[]{null, "", "  "}) {
            GarmentAsset asset = resolver.resolve("shirt_slim", "s", bucket);

            assertThat(asset.unavailableReason()).isEqualTo(UnavailableReason.SIMULATION_FAILED);
            assertThat(asset.glbUrl()).isNull();
        }
    }

    @Test
    @DisplayName("목록 파일이 없으면 기동에 실패한다")
    void failsFastWhenResourceMissing() {
        GarmentAssetResolver broken =
                new GarmentAssetResolver(new ObjectMapper(), "no_such_file.json", BASE);

        // 목록이 빈 채로 뜨면 착용 불가 조합에도 주소가 만들어져 화면에서만 깨집니다.
        assertThatThrownBy(broken::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no_such_file.json");
    }
}
