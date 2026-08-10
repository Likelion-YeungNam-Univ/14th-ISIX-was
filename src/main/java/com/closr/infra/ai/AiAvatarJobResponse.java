package com.closr.infra.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * AI 서버 작업 등록 응답 DTO. (POST /api/avatar/generate, 202 Accepted)
 *
 * <p>아바타 생성은 비동기라 이 응답에는 결과가 없습니다.
 * avatarId 를 받아 {@link AiClient#getAvatar(String)} 로 폴링하세요.
 *
 * <p>pollAfterMs 는 AI 서버가 권장하는 첫 조회 대기 시간입니다.
 * 처리에 보통 10~20초가 걸리므로 그보다 일찍 조회하면 processing 만 반환됩니다.
 *
 * <p>AI 서버는 필드명을 스네이크 케이스로 보냅니다. (avatar_id, poll_after_ms)
 * 이 DTO 는 카멜 케이스라 매핑을 지정하지 않으면 Jackson 이 값을 못 찾고
 * 예외 없이 null 을 채웁니다. 그러면 폴링 주소가 /api/avatar/null 이 되는데,
 * 응답은 200 이라 어디서 틀렸는지 드러나지 않습니다.
 * 전역 설정 대신 이 DTO 에만 지정합니다. 전역으로 바꾸면 프론트로 나가는
 * 응답까지 스네이크가 됩니다.
 *
 * <p>중첩 record 에는 상속되지 않아 각각에 붙여야 합니다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiAvatarJobResponse(
        boolean success,
        Data data,
        Error error
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Data(
            String avatarId,
            String status,
            Integer pollAfterMs
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Error(String code, String message) {}
}
