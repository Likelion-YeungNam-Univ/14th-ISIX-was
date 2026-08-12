package com.closr.domain.garment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 사전 계산된 GLB · 여유량 파일의 주소를 만듭니다.
 *
 * <p>의류 시뮬레이션은 1벌당 30초~2분이 걸려 런타임에 돌릴 수 없습니다. 그래서 대표
 * 체형 12구간 × 의류 6종 × 사이즈 3종을 미리 만들어 R2 에 올려두고, 요청 때는 주소만
 * 조합합니다.
 *
 * <pre>
 *   GLB     {design}_{size}__{bucket}.glb
 *   여유량  {design}_{size}__{bucket}_ease.json
 *
 *   예) tshirt_basic_m__H1B1.glb / tshirt_basic_m__H1B1_ease.json
 * </pre>
 *
 * <p><b>{@code _ease.json} 입니다.</b> {@code .json} 이 아닙니다. 그리고 사이즈는 반드시
 * 소문자입니다. R2 는 키 대소문자를 구분해서 {@code _M__} 로 조합하면 404 가 납니다.
 *
 * <p>착용 불가 조합은 {@code missing_combos.json} 을 읽어 판단합니다. <b>목록을 코드로
 * 옮겨 적지 않습니다.</b> 옮겨 적으면 216개를 재생성했을 때 어긋난 것을 아무도 알아채지
 * 못합니다. {@code body_grid.json} 과 같은 방식이고, 파일 원본은 시뮬레이션 파트의
 * {@code garment/config/missing_combos.json} 입니다. 재생성되면 파일만 교체합니다.
 */
@Slf4j
@Component
public class GarmentAssetResolver {

    public static final String DEFAULT_RESOURCE = "missing_combos.json";

    private final ObjectMapper objectMapper;
    private final String resource;
    private final String baseUrl;

    private Set<String> missingCombos;

    public GarmentAssetResolver(
            ObjectMapper objectMapper,
            @Value("${closr.missing-combos-resource:" + DEFAULT_RESOURCE + "}") String resource,
            @Value("${closr.assets.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.resource = resource;
        // 뒤에 슬래시가 붙어 오면 주소에 // 가 생겨 R2 에서 404 가 납니다.
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 착용 불가 목록을 읽습니다.
     *
     * <p>파일이 없거나 형태가 다르면 기동에 실패시킵니다. 목록을 빈 채로 뜨면 착용
     * 불가 조합에도 주소가 만들어져, 프론트 3D 로더가 R2 404 를 받고 화면에서 깨집니다.
     * 서버 로그에는 아무것도 남지 않아 원인을 찾기 어렵습니다.
     */
    @PostConstruct
    public void load() {
        JsonNode root = read();

        JsonNode missing = root.path("missing");
        if (!missing.isArray()) {
            throw new IllegalStateException(
                    "%s 에 missing 배열이 없습니다. 파일 형태가 바뀌었는지 확인하세요.".formatted(resource));
        }

        Set<String> combos = new LinkedHashSet<>();
        missing.forEach(node -> combos.add(node.asText()));
        missingCombos = Set.copyOf(combos);

        log.info("착용 불가 조합 로드 완료: {}건 {}, 에셋 베이스 {}", missingCombos.size(), missingCombos, baseUrl);
    }

    private JsonNode read() {
        try (InputStream stream = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readTree(stream);
        } catch (IOException e) {
            throw new IllegalStateException("%s 를 읽을 수 없습니다.".formatted(resource), e);
        }
    }

    /**
     * 조합 하나의 주소를 만듭니다.
     *
     * @param design 의류 디자인. 예) {@code shirt_slim}
     * @param size   사이즈. 대문자로 들어와도 소문자로 맞춥니다
     * @param bucket 체형 구간. 예) {@code H1B2}. 정하지 못했으면 {@code null}
     */
    public GarmentAsset resolve(String design, String size, String bucket) {
        if (bucket == null || bucket.isBlank()) {
            // 파일명을 조합하면 shirt_slim_m__.glb 가 되어 R2 에서 404 가 납니다.
            return GarmentAsset.unavailable(UnavailableReason.SIMULATION_FAILED);
        }

        String key = key(design, size, bucket);
        if (missingCombos.contains(key)) {
            return GarmentAsset.unavailable(UnavailableReason.TOO_SMALL);
        }
        return GarmentAsset.available(
                "%s/%s.glb".formatted(baseUrl, key),
                "%s/%s_ease.json".formatted(baseUrl, key));
    }

    static String key(String design, String size, String bucket) {
        return "%s_%s__%s".formatted(design, size.toLowerCase(Locale.ROOT), bucket);
    }
}
