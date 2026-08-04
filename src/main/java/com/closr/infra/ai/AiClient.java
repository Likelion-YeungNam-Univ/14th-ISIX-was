package com.closr.infra.ai;

import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 서버 클라이언트.
 *
 * <p>AI 파이프라인은 무거운 Python 의존성(torch·mediapipe·trimesh)을 쓰고
 * 배포·스케일링 주기도 달라 별도 FastAPI 서버로 분리했습니다.
 *
 * <pre>
 * [Spring Boot]  ──HTTP──→  [FastAPI · AI]
 * </pre>
 *
 * <p><b>아바타 생성은 비동기입니다.</b>
 * 사진 1장 처리에 10~20초가 걸려(대부분 SMPL-X 최적화) 동기 호출로는
 * 타임아웃에 걸립니다. 작업을 등록해 avatarId 를 받고 폴링하세요.
 *
 * <pre>
 * requestAvatar(...)  ──→  202 + avatarId
 * getAvatar(avatarId) ──→  processing | done | failed
 * </pre>
 *
 * <p>외부 연동은 이 클래스에 격리합니다.
 * AI 서버 장애 시 500 을 그대로 노출하지 않고 AI_SERVER_UNAVAILABLE 로 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient aiRestClient;

    /**
     * 아바타 생성 작업을 등록합니다. 결과를 기다리지 않고 즉시 반환합니다.
     *
     * @return 조회에 사용할 avatarId 가 담긴 응답
     */
    public AiAvatarJobResponse requestAvatar(MultipartFile photo, int height, int weight) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", photo.getResource());
        body.add("height", height);
        body.add("weight", weight);

        try {
            return aiRestClient.post()
                    .uri("/api/avatar/generate")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AiAvatarJobResponse.class);
        } catch (RestClientException e) {
            log.error("AI 서버 아바타 생성 요청 실패", e);
            throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
        }
    }

    /**
     * 아바타 생성 상태를 조회합니다.
     *
     * <p>status 가 processing 이면 잠시 뒤 다시 호출하세요.
     * 권장 폴링 간격은 등록 응답의 pollAfterMs 를 따릅니다.
     */
    public AiAvatarResponse getAvatar(String avatarId) {
        try {
            return aiRestClient.get()
                    .uri("/api/avatar/{avatarId}", avatarId)
                    .retrieve()
                    .body(AiAvatarResponse.class);
        } catch (RestClientException e) {
            log.error("AI 서버 아바타 조회 실패: {}", avatarId, e);
            throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
        }
    }
}
