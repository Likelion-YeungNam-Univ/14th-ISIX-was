package com.closr.domain.avatar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 아바타를 체형 12구간 중 하나에 배정합니다.
 *
 * <p>의류 시뮬레이션은 1벌당 30초~2분이 걸려 런타임에 돌릴 수 없습니다.
 * 그래서 대표 체형 12종에 미리 옷을 입혀 두고, 사용자를 가장 가까운 구간에 배정해
 * 그 결과물을 보여줍니다.
 *
 * <p><b>배정 결과는 "보여줄 몸"을 고르는 데만 씁니다.</b>
 * 사이즈 추천에 구간 대표 치수를 쓰면 안 됩니다. 격자가 키를 3단계, 가슴을 4단계로
 * 양자화해 실제 몸과 키 최대 4.0cm · 가슴 최대 3.4cm 어긋나기 때문입니다.
 * 사이즈 판정은 반드시 사용자별 실측 치수로 해야 합니다.
 * (원본 파일의 {@code usage.do_not} 에 명시된 제약입니다)
 *
 * <p>경계값과 구간 정의는 {@code body_grid.json} 에서 읽습니다. 이 파일은
 * 아바타 · 의류 · 백엔드 세 파트가 공유하므로 숫자를 코드로 옮겨 적지 않습니다.
 * 옮겨 적으면 원본이 바뀌었을 때 어긋난 것을 아무도 알아채지 못합니다.
 */
@Slf4j
@Component
public class BodyGridMatcher {

    static final String DEFAULT_RESOURCE = "body_grid.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 2;
    private static final int EXPECTED_BUCKET_COUNT = 12;

    private final ObjectMapper objectMapper;
    private final String resource;

    /** 키 경계 (cm). 경계값 자체는 위쪽 구간에 포함됩니다. */
    private List<Double> heightBounds;

    /** 가슴둘레 경계 (cm). 경계값 자체는 위쪽 구간에 포함됩니다. */
    private List<Double> chestBounds;

    private Set<String> bucketIds;

    public BodyGridMatcher(
            ObjectMapper objectMapper,
            @Value("${closr.body-grid-resource:" + DEFAULT_RESOURCE + "}") String resource) {
        this.objectMapper = objectMapper;
        this.resource = resource;
    }

    /**
     * 격자를 읽고 검증합니다.
     *
     * <p>파일이 없거나 형태가 다르면 기동에 실패시킵니다. 잘못된 격자로 조용히 뜨면
     * 엉뚱한 체형의 옷을 보여주게 되는데, 그건 화면상 오류로 보이지 않아 발견이 늦습니다.
     */
    @PostConstruct
    void load() {
        JsonNode grid = readGrid();

        int schemaVersion = grid.path("schema_version").asInt(-1);
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "%s 의 schema_version 이 %d 입니다. 지원 버전은 %d 입니다. 격자 정의가 바뀌었는지 확인하세요."
                            .formatted(resource, schemaVersion, SUPPORTED_SCHEMA_VERSION));
        }

        JsonNode assignment = grid.path("assignment");
        heightBounds = toDoubles(assignment.path("H_bounds_cm"));
        chestBounds = toDoubles(assignment.path("B_bounds_cm"));

        bucketIds = new LinkedHashSet<>();
        grid.path("buckets").forEach(bucket -> bucketIds.add(bucket.path("id").asText()));

        if (bucketIds.size() != EXPECTED_BUCKET_COUNT) {
            throw new IllegalStateException(
                    "%s 의 구간이 %d 개입니다. %d 개여야 합니다."
                            .formatted(resource, bucketIds.size(), EXPECTED_BUCKET_COUNT));
        }

        // 경계 개수와 구간 개수가 맞아야 모든 입력이 실제 구간으로 떨어집니다.
        int expected = (heightBounds.size() + 1) * (chestBounds.size() + 1);
        if (expected != EXPECTED_BUCKET_COUNT) {
            throw new IllegalStateException(
                    "%s 의 경계값으로 만들 수 있는 구간이 %d 개인데 정의된 구간은 %d 개입니다."
                            .formatted(resource, expected, EXPECTED_BUCKET_COUNT));
        }

        log.info("체형 격자 로드 완료: 구간 {}개, 키 경계 {}, 가슴 경계 {}",
                bucketIds.size(), heightBounds, chestBounds);
    }

    /**
     * 키와 가슴둘레로 구간을 정합니다.
     *
     * @param heightCm  사용자가 입력한 키
     * @param chestCirc 아바타 계측의 {@code chest_circ}
     * @return 구간 id. 예) {@code H1B2}
     */
    public String assign(double heightCm, double chestCirc) {
        String id = "H%dB%d".formatted(
                indexOf(heightBounds, heightCm),
                indexOf(chestBounds, chestCirc));

        if (!bucketIds.contains(id)) {
            throw new IllegalStateException(
                    "구간 %s 가 %s 에 없습니다. (키 %.1f, 가슴 %.1f)"
                            .formatted(id, resource, heightCm, chestCirc));
        }
        return id;
    }

    /** 경계값 이상이면 위쪽 구간입니다. */
    private int indexOf(List<Double> bounds, double value) {
        int index = 0;
        for (double bound : bounds) {
            if (value >= bound) {
                index++;
            }
        }
        return index;
    }

    private JsonNode readGrid() {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("%s 를 읽지 못했습니다.".formatted(resource), e);
        }
    }

    private List<Double> toDoubles(JsonNode array) {
        List<Double> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asDouble()));
        if (values.isEmpty()) {
            throw new IllegalStateException("%s 의 경계값이 비어 있습니다.".formatted(resource));
        }
        return List.copyOf(values);
    }
}
