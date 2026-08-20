package com.closr.infra.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 서버 챗 요청 본문. (POST /api/chat)
 *
 * <p><b>필드 이름이 snake_case 입니다.</b> AI 서버가 파이썬이라 그쪽 표기를
 * 따릅니다. PR #26 이 이 불일치로 값이 조용히 {@code null} 이 된 건이라
 * 여기서만 이름을 맞추고 도메인에는 번지지 않게 합니다.
 *
 * <p>{@code fit_context} 는 아직 채우지 않습니다. 아바타 실측과 판정,
 * 지난 피팅을 조립하는 작업이 별건입니다. {@code null} 이면 AI 가
 * onboarding 처럼 다루므로, mode=fitting 이어도 지금은 수치를 말하지 않습니다.
 */
public record AiChatRequest(
        String mode,
        String message,
        List<Turn> history,
        Map<String, Object> fit_context
) {

    public record Turn(String role, String content) {}
}
