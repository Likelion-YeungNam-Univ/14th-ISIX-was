package com.closr.api;

import com.closr.domain.chat.dto.RequestChatDto;
import com.closr.domain.user.entity.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "Chat", description = "AI 챗봇 API")
@RequestMapping("/api/v1/chat")
public interface ChatApi {

    @Operation(
            summary = "챗봇 대화 (SSE 스트리밍)",
            description = """
                    답변을 토큰 단위로 흘려보냅니다. 응답은 `text/event-stream` 입니다.

                    ```
                    data: {"conversationId":"cv_9f21ab"}
                    data: {"delta":"어깨가 "}
                    data: {"done":true,"messageId":"msg_004"}
                    ```

                    에러는 두 경로로 나갑니다. 스트림 시작 **전** 실패(400·404)는 일반 HTTP
                    상태로, 시작 **후** 실패(502)는 스트림 안의 `error` 이벤트로 갑니다.
                    헤더가 나간 뒤에는 상태 코드를 바꿀 수 없기 때문입니다.
                    `fetch` 응답 상태를 먼저 보고 200 일 때만 스트림을 읽어주세요.
                    """
    )
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<StreamingResponseBody> chat(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Valid @RequestBody RequestChatDto request
    );
}
