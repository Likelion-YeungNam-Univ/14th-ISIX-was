package com.closr.domain.chat.dto;

import com.closr.domain.chat.entity.ChatMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 챗봇 요청.
 *
 * <p>{@code conversationId} 가 없으면 새 대화를 엽니다. 이어지는 대화면 이전
 * 응답의 첫 이벤트로 받은 값을 그대로 보냅니다.
 */
public record RequestChatDto(

        @NotNull(message = "mode 는 필수입니다")
        ChatMode mode,

        String conversationId,

        // mode=fitting 이면 필수입니다. 치수 없이 사이즈를 답하면 없는 수치를 지어냅니다.
        Long avatarId,

        // 의류 목록·상세가 내려주는 PK 입니다. 명세 예시에는 "shirt_slim" 으로
        // 적혀 있지만 백엔드가 design 으로 조회하는 경로가 없고, 프론트가 들고
        // 있는 값도 PK 입니다. AI 에는 여기서 design 문자열로 바꿔 보냅니다.
        Long garmentId,

        // s | m | l (소문자). 없으면 추천 사이즈를 씁니다.
        String size,

        @NotBlank(message = "message 는 필수입니다")
        @Size(max = 500, message = "발화는 500자를 넘을 수 없습니다")
        String message
) {}
