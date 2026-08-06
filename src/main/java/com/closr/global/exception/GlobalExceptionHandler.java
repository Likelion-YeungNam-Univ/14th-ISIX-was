package com.closr.global.exception;

import com.closr.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리.
 *
 * <p>컨트롤러에서 try-catch 를 사용하지 않습니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** application/json;charset=UTF-8 */
    private static final MediaType JSON_UTF8 =
            new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("CustomException: {} - {}", code.name(), code.getMessage());
        return errorResponse(code);
    }

    /** 요청 본문 검증 실패. 어느 필드가 문제인지 함께 반환합니다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField())
                .orElse(null);
        log.warn("Validation failed: field={}", field);
        return errorResponse(ErrorCode.INVALID_INPUT, field);
    }

    /**
     * 존재하지 않는 경로.
     *
     * <p>Spring Boot 3.2 부터 매칭되는 핸들러가 없으면 정적 리소스 탐색으로 넘어가
     * {@link NoResourceFoundException} 이 올라옵니다. 아래 {@code handleUnexpected} 가
     * 이것까지 잡아 모든 404 가 500 으로 나가던 것을 막습니다.
     *
     * <p>존재하지 않는 경로 요청은 서버 잘못이 아니므로 error 로 남기지 않습니다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        log.debug("No handler: {}", e.getMessage());
        return errorResponse(ErrorCode.NOT_FOUND);
    }

    /**
     * {@code @RequestParam} · {@code @PathVariable} 의 제약 위반.
     *
     * <p>본문 검증 실패는 {@link MethodArgumentNotValidException} 으로 오지만,
     * 파라미터 검증 실패는 이쪽으로 옵니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String field = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> {
                    // propertyPath 는 "createAvatar.height" 형태라 마지막 마디만 씁니다.
                    String path = violation.getPropertyPath().toString();
                    return path.substring(path.lastIndexOf('.') + 1);
                })
                .orElse(null);
        log.warn("Constraint violation: field={}", field);
        return errorResponse(ErrorCode.INVALID_INPUT, field);
    }

    /** 경로변수 · 쿼리파라미터의 타입이 맞지 않는 경우. 예) {@code /garments/abc/fit} */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: name={}, value={}", e.getName(), e.getValue());
        return errorResponse(ErrorCode.INVALID_INPUT, e.getName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSize(MaxUploadSizeExceededException e) {
        return errorResponse(ErrorCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return errorResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return errorResponse(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 오류 응답을 만듭니다.
     *
     * <p>Content-Type 에 charset 을 명시합니다. 생략하면 클라이언트가 인코딩을
     * 추측하는데, Latin-1 로 추측하면 한글 메시지가 깨져서 표시됩니다.
     */
    private ResponseEntity<ApiResponse<Void>> errorResponse(ErrorCode code) {
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(JSON_UTF8)
                .body(ApiResponse.fail(code));
    }

    /** 문제가 된 필드명을 함께 담는 오류 응답. */
    private ResponseEntity<ApiResponse<Void>> errorResponse(ErrorCode code, String field) {
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(JSON_UTF8)
                .body(ApiResponse.fail(code, field));
    }
}
