package com.closer.global.common;

import com.closer.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 응답 래퍼.
 *
 * <p>프론트와의 약속입니다. 모든 응답을 이 형태로 감쌉니다.
 * AI 서버(FastAPI)도 동일한 봉투 구조를 사용합니다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorBody error;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorBody(errorCode.name(), errorCode.getMessage())
        );
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String field) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorBody(errorCode.name(), errorCode.getMessage(), field)
        );
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ErrorBody {
        private String code;
        private String message;
        private String field;

        public ErrorBody(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorBody(String code, String message, String field) {
            this.code = code;
            this.message = message;
            this.field = field;
        }
    }
}
