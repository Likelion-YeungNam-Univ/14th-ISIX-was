package com.closr.domain.chat.controller;

import com.closr.api.ChatApi;
import com.closr.domain.chat.dto.RequestChatDto;
import com.closr.domain.chat.service.ChatService;
import com.closr.domain.user.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 챗봇 컨트롤러.
 *
 * <p>대화 기록 조회는 {@code ChatHistoryController} 에 있습니다. 이쪽만 SSE 라
 * 응답 타입이 달라 분리했습니다.
 */
@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ChatService chatService;

    @Override
    public ResponseEntity<StreamingResponseBody> chat(Session session, RequestChatDto request) {
        // 검증은 여기서 끝납니다. relay 안에서 던지는 예외는 아직 헤더가 나가지
        // 않은 상태라 상태 코드로 알릴 수 있습니다.
        StreamingResponseBody body = chatService.relay(session, request);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                // 프록시가 응답을 모아 두면 delta 가 완료 시점에 한꺼번에 도착합니다.
                // 스트리밍이 통째로 죽는데 로컬에서는 드러나지 않습니다.
                .header("X-Accel-Buffering", "no")
                .body(body);
    }
}
