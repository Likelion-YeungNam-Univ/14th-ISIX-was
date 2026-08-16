package com.closr.domain.chat.controller;

import com.closr.api.ChatHistoryApi;
import com.closr.domain.chat.dto.ResponseChatHistoryDto;
import com.closr.domain.chat.dto.ResponseChatSummaryDto;
import com.closr.domain.chat.service.ChatHistoryService;
import com.closr.domain.chat.service.ChatSummaryService;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대화 기록 조회 컨트롤러.
 *
 * <p>매핑과 스웨거 명세는 {@link ChatHistoryApi} 에 있습니다.
 *
 * <p>챗봇 요청(POST /api/v1/chat)은 SSE 스트리밍이라 별도 컨트롤러로 분리합니다.
 * 응답 형태가 달라 같은 클래스에 두면 공통 래퍼 규약이 섞입니다.
 */
@RestController
@RequiredArgsConstructor
public class ChatHistoryController implements ChatHistoryApi {

    private final ChatHistoryService chatHistoryService;
    private final ChatSummaryService chatSummaryService;

    @Override
    public ResponseEntity<ApiResponse<ResponseChatHistoryDto>> getHistory(
            @RequestAttribute("session") Session session, String conversationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(chatHistoryService.findHistory(session, conversationId)));
    }

    @Override
    public ResponseEntity<ApiResponse<ResponseChatSummaryDto>> getSummary(
            @RequestAttribute("session") Session session, String conversationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(chatSummaryService.findSummary(session, conversationId)));
    }
}
