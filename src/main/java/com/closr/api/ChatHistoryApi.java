package com.closr.api;

import com.closr.domain.chat.dto.ResponseChatHistoryDto;
import com.closr.domain.chat.dto.ResponseChatSummaryDto;
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

@Tag(name = "Chat", description = "AI 챗봇 API")
@RequestMapping("/api/v1/chat")
public interface ChatHistoryApi {

    @Operation(summary = "대화 기록 조회",
            description = "저장된 대화를 오래된 순으로 반환합니다. 새로고침 후 챗봇 화면을 복원할 때 사용합니다. "
                    + "다른 세션의 대화는 존재 여부를 감추기 위해 404 로 응답합니다.")
    @GetMapping("/{conversationId}")
    ResponseEntity<ApiResponse<ResponseChatHistoryDto>> getHistory(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "대화 ID", example = "cv_9f21ab7c4d20")
            @PathVariable("conversationId") String conversationId
    );

    @Operation(summary = "상담 요약 조회",
            description = """
                    상담이 끝난 뒤 보여주는 "오늘의 상담" 입니다. 이 상담에서 비교한 옷과
                    대화에서 알아낸 취향을 함께 돌려줍니다.

                    저장된 값을 조립할 뿐 다시 계산하지 않습니다. 판정 수치는 사용자가 피팅
                    화면에서 본 값과 같습니다.

                    `items` 는 본 순서대로이고 최대 5벌입니다. 두 벌 이상일 때만 그중 하나에
                    `bestFit` 이 붙습니다. `headline` 과 `preference` 는 보여줄 것이 없으면
                    `null` 이므로, 프론트는 셋으로 화면 노출 여부를 정하면 됩니다.

                    옷을 하나도 안 본 상담(온보딩)은 `items` 가 빈 배열입니다.
                    다른 세션의 대화는 존재 여부를 감추기 위해 404 로 응답합니다.""")
    @GetMapping("/{conversationId}/summary")
    ResponseEntity<ApiResponse<ResponseChatSummaryDto>> getSummary(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "대화 ID", example = "cv_9f21ab7c4d20")
            @PathVariable("conversationId") String conversationId
    );
}
