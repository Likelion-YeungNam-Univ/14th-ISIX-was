package com.closer.infra.ai;

/**
 * AI 서버 작업 등록 응답 DTO. (POST /api/avatar/generate, 202 Accepted)
 *
 * <p>아바타 생성은 비동기라 이 응답에는 결과가 없습니다.
 * avatarId 를 받아 {@link AiClient#getAvatar(String)} 로 폴링하세요.
 *
 * <p>pollAfterMs 는 AI 서버가 권장하는 첫 조회 대기 시간입니다.
 * 처리에 보통 10~20초가 걸리므로 그보다 일찍 조회하면 processing 만 반환됩니다.
 */
public record AiAvatarJobResponse(
        boolean success,
        Data data,
        Error error
) {
    public record Data(
            String avatarId,
            String status,
            Integer pollAfterMs
    ) {}

    public record Error(String code, String message) {}
}
