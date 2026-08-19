package com.closr.domain.chat.entity;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.user.entity.Session;
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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * AI 챗봇 대화.
 *
 * <p>대화 한 건이 메시지 여러 개를 가집니다. LLM API 는 무상태라서 매 턴마다
 * 이전 대화를 전부 다시 보내야 하는데, 그 히스토리를 여기에 보관합니다.
 *
 * <p><b>AI 서버에 보관하지 않는 이유가 있습니다.</b> AI 서버는 아바타 작업
 * 상태를 프로세스 메모리에 들고 있어 재시작하면 사라집니다. 대화도 같은 곳에
 * 두면 배포할 때마다 전부 날아갑니다.
 *
 * <p>회원 개념이 없어 세션에 매답니다. 세션이 만료되면(발급 후 7일) 대화도
 * 접근할 수 없습니다.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 프론트가 들고 다니는 값입니다.
     *
     * <p>DB 기본키를 그대로 노출하면 남의 대화를 순번으로 찾아볼 수 있습니다.
     * 아바타의 jobId 와 같은 이유로 별도 문자열을 둡니다.
     */
    @Column(name = "conversation_id", nullable = false, unique = true, length = 40)
    private String conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMode mode;

    /**
     * 대화 대상 아바타. onboarding 모드면 비어 있습니다.
     *
     * <p>홈 화면 대화는 아바타 생성 이전 단계라 붙일 아바타가 없습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id")
    private Avatar avatar;

    /**
     * 대화에서 뽑은 요약. 다음 요청의 {@code fit_context.profile} 로 되돌려 보냅니다.
     *
     * <p>AI 가 {@code done} 이벤트에 실어 보내는 값을 그대로 담습니다. 항목이
     * 고정돼 있어(용도 · 신경쓰는부위 · 선호핏 · 피하는것) 자유 서술이 들어오지
     * 않습니다. 첫 대화에서는 {@code null} 입니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> summary;

    /**
     * 뒤늦게 아바타를 붙입니다. 이미 붙어 있으면 그대로 둡니다.
     *
     * <p>홈에서 시작한 대화는 아바타 이전 화면이라 {@code null} 로 열립니다.
     * 사용자가 그 대화를 이어서 피팅룸에 들어오면 그때 아바타가 정해집니다.
     * <b>여기서 붙이지 않으면 상담 요약이 빈 값으로 나갑니다</b> — 요약은
     * 대화에 매달린 아바타로 피팅 기록을 찾습니다.
     *
     * <p>바꾸지는 않습니다. 아바타를 옮겨 가며 대화하면 어느 몸의 기록인지
     * 알 수 없어져, 첫 아바타를 기준으로 둡니다.
     */
    public void linkAvatarIfAbsent(Avatar avatar) {
        if (this.avatar == null) {
            this.avatar = avatar;
        }
    }

    /** 새 요약으로 갈아 끼웁니다. 이전 값과 병합하지 않습니다 — 매 턴 전체를 다시 뽑습니다. */
    public void updateSummary(Map<String, Object> summary) {
        this.summary = summary;
    }

    @Builder
    private Conversation(String conversationId, Session session, ChatMode mode, Avatar avatar) {
        this.conversationId = conversationId;
        this.session = session;
        this.mode = mode;
        this.avatar = avatar;
    }

    /** 이 세션이 소유한 대화인지. 아니면 조회를 거부합니다. */
    public boolean isOwnedBy(Session other) {
        return session.getId().equals(other.getId());
    }
}
