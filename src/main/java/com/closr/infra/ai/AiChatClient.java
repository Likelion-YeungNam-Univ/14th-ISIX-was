package com.closr.infra.ai;

import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 서버 챗 스트림 클라이언트.
 *
 * <p>{@link AiClient} 와 분리한 이유는 응답을 버퍼링하지 않기 때문입니다.
 * {@code retrieve()} 는 본문을 다 읽은 뒤 돌려주므로 스트리밍이 성립하지
 * 않습니다. {@code exchange()} 로 {@code InputStream} 을 직접 읽습니다.
 *
 * <p>줄 단위로 읽습니다. TCP 청크 경계는 SSE 프레임 경계와 무관해서, 바이트가
 * 오는 대로 넘기면 {@code data: {"delta":"어} 처럼 반쪽 JSON 이 나갑니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClient {

    private final RestClient aiRestClient;

    /**
     * 챗 응답을 한 줄씩 넘겨줍니다.
     *
     * <p>첫 줄을 받기 전에 실패하면 {@link CustomException} 을 던집니다. 호출부가
     * 아직 응답 헤더를 내보내지 않은 상태라 상태 코드로 알릴 수 있습니다.
     *
     * @param onLine 빈 줄을 제외한 각 줄. {@code data: {...}} 형태입니다
     */
    public void stream(AiChatRequest request, Consumer<String> onLine) {
        try {
            aiRestClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(request)
                    .exchange((req, response) -> {
                        if (response.getStatusCode().isError()) {
                            // AI 가 스트림을 열기 전에 거절한 경우입니다.
                            // 본문에 공통 봉투가 담겨 있지만 여기서는 코드만 봅니다.
                            log.error("AI 챗 요청이 거절되었습니다: {}", response.getStatusCode());
                            throw new CustomException(ErrorCode.CHAT_UPSTREAM_ERROR);
                        }
                        readLines(response.getBody(), onLine);
                        // exchange 는 반환값을 요구합니다. 여기서 쓸 값이 없어
                        // 상수를 돌려주고 버립니다. null 을 돌려주면 호출부가
                        // 의미 없는 null 검사를 하게 됩니다.
                        return Boolean.TRUE;
                    });
        } catch (RestClientException e) {
            // CustomException 은 RestClientException 의 형제라 여기 걸리지 않고
            // 그대로 올라갑니다. 따로 재던질 필요가 없습니다.
            log.error("AI 챗 서버에 연결할 수 없습니다", e);
            throw new CustomException(ErrorCode.CHAT_UPSTREAM_ERROR);
        }
    }

    private void readLines(java.io.InputStream body, Consumer<String> onLine) {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    onLine.accept(line);
                }
            }
        } catch (IOException e) {
            // 여기까지 왔으면 이미 일부 토큰이 프론트로 나갔을 수 있습니다.
            // 상태 코드를 바꿀 수 없으므로 호출부가 스트림 안에 error 를 넣습니다.
            log.error("AI 챗 스트림이 중단되었습니다", e);
            throw new CustomException(ErrorCode.CHAT_UPSTREAM_ERROR);
        }
    }
}
