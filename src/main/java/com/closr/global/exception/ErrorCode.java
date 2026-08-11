package com.closr.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 오류 코드.
 *
 * <p>프론트가 이 코드로 안내 문구를 매핑합니다.
 * "실패했습니다"로 끝내지 않고 원인과 개선 방법을 함께 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 세션 — 회원 가입 · 로그인 없이 게스트 세션만 사용합니다.
    // 소셜 로그인 · JWT 관련 코드는 회원 기능을 쓰지 않기로 하면서 제거했습니다.
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "세션이 필요합니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 새로고침 후 다시 시도해주세요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // 아바타 — AI 서버에서 전달받은 코드를 그대로 사용
    NO_PERSON_DETECTED(HttpStatus.UNPROCESSABLE_ENTITY,
            "사진에서 사람을 찾지 못했습니다. 전신이 모두 나오도록 다시 촬영해주세요"),
    POSE_NOT_FRONTAL(HttpStatus.UNPROCESSABLE_ENTITY,
            "정면 자세가 아닙니다. 카메라를 정면으로 바라봐주세요"),
    BODY_TRUNCATED(HttpStatus.UNPROCESSABLE_ENTITY,
            "전신이 나오지 않았습니다. 머리끝부터 발끝까지 나오게 촬영해주세요"),
    LOW_CONFIDENCE(HttpStatus.UNPROCESSABLE_ENTITY,
            "인식 정확도가 낮습니다. 몸선이 드러나는 옷으로 다시 촬영해주세요"),
    AVATAR_NOT_FOUND(HttpStatus.NOT_FOUND, "아바타를 찾을 수 없습니다"),
    AVATAR_NOT_READY(HttpStatus.CONFLICT,
            "아바타 생성이 아직 끝나지 않았습니다. 잠시 후 다시 시도해주세요"),
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다"),

    // 의류 · 피팅
    GARMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "의류를 찾을 수 없습니다"),
    FITTING_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "해당 조합은 준비 중입니다. 다른 사이즈를 선택해주세요"),

    // AI 상담
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다"),

    // 외부 연동
    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "아바타 생성 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요"),

    // 공통
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다"),
    UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
