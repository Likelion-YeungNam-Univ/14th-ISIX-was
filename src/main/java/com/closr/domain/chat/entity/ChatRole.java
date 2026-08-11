package com.closr.domain.chat.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대화 발화자.
 *
 * <p>AI 서버로 히스토리를 보낼 때 이 값을 그대로 씁니다. LLM API 가 소문자
 * user · assistant 를 요구하므로 API 표기는 소문자로 고정합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ChatRole {

    USER("user"),
    ASSISTANT("assistant");

    @JsonValue
    private final String value;
}
