package com.closr.infra.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 서버 조회 응답 DTO. (GET /api/avatar/{avatarId})
 *
 * <p>아바타 생성은 비동기입니다. POST 로 작업을 등록해 avatarId 를 받고,
 * 이 응답의 status 가 done 이 될 때까지 폴링합니다.
 *
 * <p>status 가 done 일 때만 result 가 채워집니다.
 * processing 이면 result 는 null 이고, failed 이면 errorMessage 에 사유가 담깁니다.
 *
 * <p>measurements 의 키 12개는 AI 파트가 정의한 문자열을 그대로 사용합니다.
 * 임의로 바꾸면 프론트까지 함께 깨집니다.
 */
public record AiAvatarResponse(
        boolean success,
        Data data,
        Error error
) {
    public record Data(
            String avatarId,
            String status,
            Result result,
            String errorMessage
    ) {}

    public record Result(
            String glbUrl,
            String bodyBucket,
            Map<String, Double> measurements,
            Double confidence,
            List<String> warnings
    ) {}

    public record Error(String code, String message) {}

    /** 작업이 아직 진행 중인지. */
    public boolean isProcessing() {
        return data != null && "processing".equals(data.status());
    }

    /** 작업이 성공적으로 끝났는지. */
    public boolean isDone() {
        return data != null && "done".equals(data.status());
    }
}
