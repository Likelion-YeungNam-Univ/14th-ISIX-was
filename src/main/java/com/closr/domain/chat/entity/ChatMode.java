package com.closr.domain.chat.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 챗봇 모드.
 *
 * <p>배치된 화면에 따라 챗봇이 아는 정보가 다릅니다. 홈에서는 아바타가 없어
 * 치수를 모르고, 피팅룸에서는 치수와 핏 리포트를 근거로 답합니다. AI 서버가
 * 이 값으로 시스템 프롬프트를 갈아끼우므로 임의로 늘리면 안 됩니다.
 *
 * <p>DB 에는 이름 그대로(ONBOARDING · FITTING) 저장하고, API 로는 소문자를
 * 내보냅니다. AI 서버가 소문자를 받도록 명세에 적혀 있습니다.
 */
@Getter
@RequiredArgsConstructor
public enum ChatMode {

    /** 홈. 아바타 이전 단계라 치수를 모릅니다. */
    ONBOARDING("onboarding"),

    /** 피팅룸. 치수와 핏 리포트를 근거로 답합니다. */
    FITTING("fitting");

    @JsonValue
    private final String value;

    public static ChatMode from(String value) {
        for (ChatMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("알 수 없는 챗봇 모드입니다: " + value);
    }
}
