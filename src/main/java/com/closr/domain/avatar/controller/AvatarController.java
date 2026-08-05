package com.closr.domain.avatar.controller;

import com.closr.api.AvatarApi;
import com.closr.domain.avatar.dto.ResponseAvatarJobDto;
import com.closr.domain.avatar.dto.ResponseAvatarStatusDto;
import com.closr.global.common.ApiResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 아바타 목 컨트롤러.
 *
 * <p><b>임시 스텁입니다.</b> AI 서버 호출 없이 고정값만 반환합니다.
 * 업로드한 사진 · 키 · 몸무게는 검증도 저장도 하지 않고 그대로 버립니다.
 *
 * <p>실제 흐름은 등록 후 폴링이지만, 목에서는 조회가 항상 즉시 {@code done} 을
 * 반환합니다. 프론트가 로딩 화면을 확인하려면 아래 상수를 {@code processing} 으로
 * 바꿔서 띄우면 됩니다.
 *
 * <p>매핑과 스웨거 명세는 {@link AvatarApi} 에 있습니다.
 */
@Validated
@RestController
public class AvatarController implements AvatarApi {

    private static final String MOCK_JOB_ID = "mock-job-0001";
    private static final Long MOCK_AVATAR_ID = 1L;
    private static final String STATUS_DONE = "done";

    /**
     * 165cm · 55kg 여성 검증 결과입니다.
     *
     * <p>키 12개는 AI 파트가 정의한 문자열을 그대로 씁니다. 임의로 바꾸면 프론트까지 깨집니다.
     * 순서를 유지하려고 {@link LinkedHashMap} 을 씁니다.
     */
    private static final Map<String, Double> MOCK_MEASUREMENTS = createMockMeasurements();

    private static Map<String, Double> createMockMeasurements() {
        Map<String, Double> measurements = new LinkedHashMap<>();
        measurements.put("shoulder_width", 40.1);
        measurements.put("chest_circ", 87.2);
        measurements.put("waist_circ", 65.5);
        measurements.put("hip_circ", 94.9);
        measurements.put("neck_circ", 31.8);
        measurements.put("arm_circ", 25.9);
        measurements.put("thigh_circ", 56.3);
        measurements.put("back_length", 39.8);
        measurements.put("sleeve_length", 54.3);
        measurements.put("inseam", 74.0);
        measurements.put("total_length", 139.6);
        measurements.put("front_width", 30.6);
        return Collections.unmodifiableMap(measurements);
    }

    /** 사진 · 키 · 몸무게를 받기만 하고 버립니다. jobId 는 항상 같습니다. */
    @Override
    public ResponseEntity<ApiResponse<ResponseAvatarJobDto>> createAvatar(
            MultipartFile photo, int height, int weight) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(new ResponseAvatarJobDto(MOCK_JOB_ID)));
    }

    /** jobId 가 무엇이든 완료된 결과를 돌려줍니다. */
    @Override
    public ResponseEntity<ApiResponse<ResponseAvatarStatusDto>> getAvatarStatus(String jobId) {
        return ResponseEntity.ok(ApiResponse.ok(
                new ResponseAvatarStatusDto(STATUS_DONE, MOCK_AVATAR_ID, MOCK_MEASUREMENTS)
        ));
    }
}