package com.closr.domain.chat.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.user.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 대화에 아바타를 뒤늦게 붙이는 동작.
 *
 * <p>홈에서 시작한 대화는 아바타 이전 화면이라 비어 있습니다. 사용자가 그 대화를
 * 이어서 피팅룸에 들어오면 그때 아바타가 정해집니다. <b>붙이지 않으면 상담 요약이
 * 빈 값으로 나갑니다</b> — 요약은 대화에 매달린 아바타로 피팅 기록을 찾습니다.
 */
class ConversationTest {

    @Test
    @DisplayName("비어 있으면 아바타를 붙인다")
    void linksWhenAbsent() {
        Conversation conversation = Conversation.builder()
                .conversationId("cv_home")
                .session(Mockito.mock(Session.class))
                .mode(ChatMode.ONBOARDING)
                .avatar(null)
                .build();
        Avatar avatar = Mockito.mock(Avatar.class);

        conversation.linkAvatarIfAbsent(avatar);

        assertThat(conversation.getAvatar()).isSameAs(avatar);
    }

    @Test
    @DisplayName("이미 붙어 있으면 바꾸지 않는다")
    void keepsTheFirstAvatar() {
        // 아바타를 옮겨 가며 대화하면 어느 몸의 기록인지 알 수 없어집니다.
        Avatar first = Mockito.mock(Avatar.class);
        Avatar second = Mockito.mock(Avatar.class);
        Conversation conversation = Conversation.builder()
                .conversationId("cv_fitting")
                .session(Mockito.mock(Session.class))
                .mode(ChatMode.FITTING)
                .avatar(first)
                .build();

        conversation.linkAvatarIfAbsent(second);

        assertThat(conversation.getAvatar()).isSameAs(first);
    }
}
