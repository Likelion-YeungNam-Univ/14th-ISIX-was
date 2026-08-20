package com.closr.domain.chat.repository;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.chat.entity.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationId(String conversationId);

    /**
     * 이 아바타에 연결된 대화. 아바타를 지울 때 연결을 끊으려고 씁니다.
     *
     * <p>대화까지 지우지는 않습니다 — 사용자가 나눈 말은 아바타와 별개로
     * 남아야 하고, 요약에 담긴 취향(용도 · 선호 핏)은 몸이 바뀌어도 유효합니다.
     */
    List<Conversation> findAllByAvatar(Avatar avatar);
}
