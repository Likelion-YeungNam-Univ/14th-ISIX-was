package com.closr.domain.fitting.controller;

import com.closr.api.FittingApi;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.fitting.dto.ResponseSizeOptionsDto;
import com.closr.global.common.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피팅 목 컨트롤러.
 *
 * <p><b>임시 스텁입니다.</b> 사전 계산 GLB 조회 없이 고정값만 반환합니다.
 * avatarId · garmentId 가 무엇이든 같은 추천 결과를 돌려줍니다.
 *
 * <p>여유량은 165cm · 55kg 검증 치수를 기준으로 잡았습니다.
 * 양수가 여유, 음수면 조이는 부위입니다. M 이 추천 사이즈이고
 * S 는 어깨 · 소매가 조이며 L 은 가슴 · 허리 · 엉덩이가 남도록 잡았습니다.
 *
 * <p>부위 키는 아바타 치수와 같은 이름을 씁니다. 프론트가 두 응답을 나란히 매칭합니다.
 *
 * <p>매핑과 스웨거 명세는 {@link FittingApi} 에 있습니다.
 */
@RestController
public class FittingController implements FittingApi {

    private static final String MOCK_RECOMMENDED_SIZE = "M";
    private static final String MOCK_RECOMMENDATION_REASON =
            "가슴둘레 87.2cm 기준 M 사이즈가 가장 잘 맞습니다. "
                    + "어깨와 허리에 여유가 남아 활동하기 편하고, 소매 길이도 손목에 맞게 떨어집니다.";

    /** 실제 GLB 가 준비되기 전까지 쓰는 가짜 주소입니다. */
    private static final String MOCK_MODEL_URL_FORMAT = "https://mock.closr.example/fit/%s.glb";

    /** 여유량 판정 경계입니다. 0 미만이면 조이고, 8cm 를 넘으면 남는 것으로 봅니다. */
    private static final double LOOSE_THRESHOLD = 8.0;

    private static final String VERDICT_TIGHT = "tight";
    private static final String VERDICT_GOOD = "good";
    private static final String VERDICT_LOOSE = "loose";

    private static final ResponseSizeOptionsDto MOCK_SIZES = createMockSizes();

    private static ResponseSizeOptionsDto createMockSizes() {
        return new ResponseSizeOptionsDto(
                createSizeDetail("S", -0.8, 2.8, 5.5, 1.1, -0.3),
                createSizeDetail("M", 1.2, 6.8, 9.5, 5.1, 0.7),
                createSizeDetail("L", 3.2, 10.8, 13.5, 9.1, 1.7)
        );
    }

    private static ResponseSizeDetailDto createSizeDetail(
            String size,
            double shoulderWidth,
            double chestCirc,
            double waistCirc,
            double hipCirc,
            double sleeveLength) {
        List<ResponseFitPartDto> parts = List.of(
                createPart("shoulder_width", shoulderWidth),
                createPart("chest_circ", chestCirc),
                createPart("waist_circ", waistCirc),
                createPart("hip_circ", hipCirc),
                createPart("sleeve_length", sleeveLength)
        );
        return new ResponseSizeDetailDto(
                String.format(MOCK_MODEL_URL_FORMAT, size),
                parts,
                MOCK_RECOMMENDED_SIZE.equals(size)
        );
    }

    private static ResponseFitPartDto createPart(String part, double ease) {
        return new ResponseFitPartDto(part, ease, verdictOf(ease));
    }

    private static String verdictOf(double ease) {
        if (ease < 0) {
            return VERDICT_TIGHT;
        }
        if (ease > LOOSE_THRESHOLD) {
            return VERDICT_LOOSE;
        }
        return VERDICT_GOOD;
    }

    /** avatarId 는 받기만 하고 버립니다. garmentId 는 요청값을 그대로 되돌려줍니다. */
    @Override
    public ResponseEntity<ApiResponse<ResponseFittingDto>> getFitting(Long avatarId, Long garmentId) {
        return ResponseEntity.ok(ApiResponse.ok(new ResponseFittingDto(
                garmentId,
                MOCK_SIZES,
                MOCK_RECOMMENDED_SIZE,
                MOCK_RECOMMENDATION_REASON
        )));
    }
}
