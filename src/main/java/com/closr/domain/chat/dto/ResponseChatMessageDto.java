package com.closr.domain.chat.dto;

import com.closr.domain.chat.entity.ChatMessage;
import com.closr.domain.chat.entity.ChatRole;
import java.time.LocalDateTime;

public record ResponseChatMessageDto(
        String messageId,
        ChatRole role,
        String content,
        LocalDateTime createdAt
) {
    public static ResponseChatMessageDto from(ChatMessage message) {
        return new ResponseChatMessageDto(
                message.getMessageId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
