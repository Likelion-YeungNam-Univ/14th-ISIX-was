package com.closr.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AI 서버 응답을 DTO 로 읽는 매핑 테스트.
 *
 * <p>AI 서버는 스네이크 케이스로 보내고 DTO 는 카멜 케이스입니다.
 * 매핑이 어긋나면 Jackson 이 예외 없이 null 을 채우고, 응답은 200 으로 나가
 * 어디서 틀렸는지 드러나지 않습니다. 그래서 값이 실제로 채워지는지 검사합니다.
 *
 * <p>아래 JSON 은 배포된 AI 서버(POST /api/avatar/generate,
 * GET /api/avatar/{id})의 응답을 그대로 옮긴 것입니다.
 */
class AiAvatarResponseMappingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("작업 등록 응답에서 avatarId 를 읽는다")
    void 작업_등록_응답_매핑() throws Exception {
        String json = """
                {
                  "success": true,
                  "data": {
                    "avatar_id": "av_ec1e5022af9a",
                    "status": "processing",
                    "poll_after_ms": 3000
                  },
                  "error": null
                }
                """;

        AiAvatarJobResponse res = mapper.readValue(json, AiAvatarJobResponse.class);

        assertThat(res.success()).isTrue();
        // avatarId 가 null 이면 폴링 주소가 /api/avatar/null 이 된다
        assertThat(res.data().avatarId()).isEqualTo("av_ec1e5022af9a");
        assertThat(res.data().status()).isEqualTo("processing");
        assertThat(res.data().pollAfterMs()).isEqualTo(3000);
    }

    @Test
    @DisplayName("완료 응답에서 glbUrl 과 치수 12개를 읽는다")
    void 완료_응답_매핑() throws Exception {
        String json = """
                {
                  "success": true,
                  "data": {
                    "avatar_id": "av_ec1e5022af9a",
                    "status": "done",
                    "result": {
                      "glb_url": "/static/avatars/av_ec1e5022af9a.glb",
                      "body_bucket": "H1B0",
                      "measurements": {
                        "shoulder_width": 45.6, "chest_circ": 85.3, "waist_circ": 61.5,
                        "hip_circ": 97.1, "neck_circ": 30.5, "arm_circ": 27.2,
                        "thigh_circ": 57.8, "back_length": 39.7, "sleeve_length": 48.8,
                        "inseam": 73.9, "total_length": 139.3, "front_width": 30.5
                      },
                      "confidence": 0.718,
                      "warnings": []
                    },
                    "error_message": null
                  },
                  "error": null
                }
                """;

        AiAvatarResponse res = mapper.readValue(json, AiAvatarResponse.class);

        assertThat(res.isDone()).isTrue();
        assertThat(res.data().avatarId()).isEqualTo("av_ec1e5022af9a");
        // glbUrl 이 null 이면 프론트 3D 뷰어에 넣을 주소가 없어진다
        assertThat(res.data().result().glbUrl())
                .isEqualTo("/static/avatars/av_ec1e5022af9a.glb");
        assertThat(res.data().result().bodyBucket()).isEqualTo("H1B0");
        assertThat(res.data().result().confidence()).isEqualTo(0.718);
        // 치수 키 12개는 AI 파트가 정의한 계약이라 개수까지 확인한다
        assertThat(res.data().result().measurements())
                .hasSize(12)
                .containsEntry("chest_circ", 85.3);
    }

    @Test
    @DisplayName("실패 응답에서 errorMessage 를 읽는다")
    void 실패_응답_매핑() throws Exception {
        String json = """
                {
                  "success": true,
                  "data": {
                    "avatar_id": "av_ec1e5022af9a",
                    "status": "failed",
                    "result": null,
                    "error_message": "사진에서 사람을 찾지 못했습니다"
                  },
                  "error": null
                }
                """;

        AiAvatarResponse res = mapper.readValue(json, AiAvatarResponse.class);

        assertThat(res.isDone()).isFalse();
        assertThat(res.isProcessing()).isFalse();
        // 사유가 null 이면 사용자에게 왜 실패했는지 알려줄 수 없다
        assertThat(res.data().errorMessage()).isEqualTo("사진에서 사람을 찾지 못했습니다");
    }
}
