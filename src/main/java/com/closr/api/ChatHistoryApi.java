package com.closr.api;

import com.closr.domain.chat.dto.ResponseChatHistoryDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Chat", description = "AI 상담 API")
@RequestMapping("/api/v1/chat")
public interface ChatHistoryApi {

    @Operation(summary = "대화 기록 조회",
            description = "저장된 대화를 오래된 순으로 반환합니다. 새로고침 후 상담 화면을 복원할 때 사용합니다. "
                    + "다른 세션의 대화는 존재 여부를 감추기 위해 404 로 응답합니다.")
    @GetMapping("/{conversationId}")
    ResponseEntity<ApiResponse<ResponseChatHistoryDto>> getHistory(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "대화 ID", example = "cv_9f21ab7c4d20")
            @PathVariable("conversationId") String conversationId
    );
}
