package com.closr.domain.chat.repository;

import com.closr.domain.chat.entity.ChatMessage;
import com.closr.domain.chat.entity.Conversation;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 화면 표시용. 오래된 것부터 읽어 대화 순서를 그대로 보여줍니다. */
    List<ChatMessage> findAllByConversationOrderByIdAsc(Conversation conversation);

    /**
     * AI 서버로 보낼 최근 히스토리.
     *
     * <p>최신순으로 잘라낸 뒤 호출부에서 뒤집습니다. 오래된 순으로 정렬해
     * 앞에서 자르면 최근 대화가 빠져 문맥이 끊깁니다.
     *
     * <p>전부 보내지 않는 이유는 토큰 비용입니다. 대화가 길어지면 매 턴마다
     * 전체를 다시 보내게 되어 요금이 대화 길이의 제곱으로 늘어납니다.
     */
    @Query("select m from ChatMessage m where m.conversation = :conversation "
            + "order by m.id desc")
    List<ChatMessage> findRecent(Conversation conversation, Pageable pageable);
}
