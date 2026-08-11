package com.closr.domain.chat.dto;

import com.closr.domain.chat.entity.ChatMode;
import java.util.List;

/**
 * 대화 기록 조회 응답.
 *
 * <p>새로고침 후 화면을 복원할 때 씁니다. avatarId 는 onboarding 대화면
 * null 입니다.
 */
public record ResponseChatHistoryDto(
        String conversationId,
        ChatMode mode,
        Long avatarId,
        List<ResponseChatMessageDto> messages
) {}
