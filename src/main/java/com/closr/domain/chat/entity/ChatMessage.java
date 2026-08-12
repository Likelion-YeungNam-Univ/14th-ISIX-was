package com.closr.domain.chat.entity;

import com.closr.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 메시지 한 건.
 *
 * <p>사용자 발화와 챗봇 답변을 같은 표에 담고 role 로 구분합니다. LLM API 가
 * 요구하는 형태와 같아서, 히스토리를 보낼 때 변환 없이 그대로 쓸 수 있습니다.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프론트가 참조하는 값입니다. 대화 ID 와 같은 이유로 기본키를 노출하지 않습니다. */
    @Column(name = "message_id", nullable = false, unique = true, length = 40)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    /**
     * 발화 내용.
     *
     * <p>사용자 발화는 500자로 제한하지만 챗봇 답변은 그보다 길 수 있어
     * 길이 제한을 두지 않습니다.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Builder
    private ChatMessage(String messageId, Conversation conversation,
                        ChatRole role, String content) {
        this.messageId = messageId;
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }
}
