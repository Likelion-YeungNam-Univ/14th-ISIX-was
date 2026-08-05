package com.closr.domain.fitting.controller;

import com.closr.api.FittingApi;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.global.common.ApiResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피팅 목 컨트롤러.
 *
 * <p><b>임시 스텁입니다.</b> 사전 계산 GLB 조회 없이 고정값만 반환합니다.
 * garmentId 가 무엇이든 같은 추천 결과를 돌려줍니다.
 *
 * <p>여유량은 165cm · 55kg 검증 치수에 M 사이즈를 얹었을 때를 기준으로 잡았습니다.
 * 양수가 여유, 음수면 조이는 부위입니다.
 *
 * <p>매핑과 스웨거 명세는 {@link FittingApi} 에 있습니다.
 */
@RestController
public class FittingController implements FittingApi {

    private static final String MOCK_RECOMMENDED_SIZE = "M";
    private static final String MOCK_RECOMMEND_REASON =
            "가슴둘레 87.2cm 기준 M 사이즈가 가장 잘 맞습니다. "
                    + "어깨와 허리에 여유가 남아 활동하기 편하고, 소매 길이도 손목에 맞게 떨어집니다.";

    /** 부위 키는 아바타 치수와 같은 이름을 씁니다. 프론트가 두 응답을 나란히 매칭합니다. */
    private static final Map<String, Double> MOCK_CLEARANCE = createMockClearance();

    private static Map<String, Double> createMockClearance() {
        Map<String, Double> clearance = new LinkedHashMap<>();
        clearance.put("shoulder_width", 1.2);
        clearance.put("chest_circ", 6.8);
        clearance.put("waist_circ", 9.5);
        clearance.put("hip_circ", 5.1);
        clearance.put("sleeve_length", 0.7);
        return Collections.unmodifiableMap(clearance);
    }

    @Override
    public ResponseEntity<ApiResponse<ResponseFittingDto>> getFittingRecommendation(Long garmentId) {
        return ResponseEntity.ok(ApiResponse.ok(new ResponseFittingDto(
                MOCK_RECOMMENDED_SIZE,
                MOCK_RECOMMEND_REASON,
                MOCK_CLEARANCE
        )));
    }
}
