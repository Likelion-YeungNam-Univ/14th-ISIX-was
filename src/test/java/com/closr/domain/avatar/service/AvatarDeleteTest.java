package com.closr.domain.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.chat.repository.ConversationRepository;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import com.closr.infra.ai.AiClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 아바타 삭제 테스트.
 *
 * <p>여기서 지키려는 것이 셋입니다. <b>딸린 것을 정리하는지</b>,
 * <b>남의 것을 지우지 않는지</b>, 그리고 <b>대화를 함께 잃지 않는지</b>입니다.
 *
 * <p>세 번째가 판단입니다. 요약에 담긴 용도 · 선호 핏은 몸이 바뀌어도 유효하고,
 * 사용자가 나눈 말을 아바타 삭제로 함께 잃으면 되돌릴 방법이 없습니다.
 */
class AvatarDeleteTest {

    private AvatarRepository avatarRepository;
    private FittingRecordRepository fittingRecordRepository;
    private ConversationRepository conversationRepository;
    private AvatarService service;
    private Session mine;

    @BeforeEach
    void setUp() {
        avatarRepository = Mockito.mock(AvatarRepository.class);
        fittingRecordRepository = Mockito.mock(FittingRecordRepository.class);
        conversationRepository = Mockito.mock(ConversationRepository.class);
        service = new AvatarService(avatarRepository, fittingRecordRepository,
                conversationRepository, Mockito.mock(AiClient.class));

        mine = Mockito.mock(Session.class);
        given(mine.getId()).willReturn(1L);
    }

    private Avatar ownedBy(Session session) {
        Avatar avatar = Mockito.mock(Avatar.class);
        given(avatar.getSession()).willReturn(session);
        return avatar;
    }

    private void exists(Long id, Avatar avatar) {
        given(avatarRepository.findById(id)).willReturn(Optional.of(avatar));
    }

    @Test
    @DisplayName("피팅 기록을 함께 지운다")
    void deletesFittingRecords() {
        // 남겨 두면 목록에 이름 없는 몸의 기록이 뜨고, 눌러도 판정을 다시 계산할
        // 아바타가 없어 화면이 빈 상태가 됩니다.
        Avatar avatar = ownedBy(mine);
        exists(7L, avatar);
        given(conversationRepository.findAllByAvatar(avatar)).willReturn(List.of());

        service.delete(mine, 7L);

        verify(fittingRecordRepository).deleteBySessionAndAvatar(mine, avatar);
        verify(avatarRepository).delete(avatar);
    }

    @Test
    @DisplayName("대화는 지우지 않고 연결만 끊는다")
    void unlinksConversationsButKeepsThem() {
        Avatar avatar = ownedBy(mine);
        exists(7L, avatar);
        Conversation conversation = Conversation.builder()
                .conversationId("c-1").session(mine)
                .mode(ChatMode.FITTING).avatar(avatar).build();
        given(conversationRepository.findAllByAvatar(avatar)).willReturn(List.of(conversation));

        service.delete(mine, 7L);

        assertThat(conversation.getAvatar()).isNull();
        // 지우지 않습니다. 취향 요약은 몸이 바뀌어도 유효합니다.
        verify(conversationRepository, never()).delete(conversation);
    }

    @Test
    @DisplayName("남의 아바타는 지우지 못한다")
    void refusesSomeoneElsesAvatar() {
        // 세션 토큰만으로 식별하는 서비스라, 소유 확인을 빠뜨리면 id 를 바꿔
        // 넣는 것으로 남의 아바타가 지워집니다.
        Session other = Mockito.mock(Session.class);
        given(other.getId()).willReturn(2L);
        Avatar avatar = ownedBy(other);
        exists(7L, avatar);

        assertThatThrownBy(() -> service.delete(mine, 7L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        verify(avatarRepository, never()).delete(avatar);
        verify(fittingRecordRepository, never())
                .deleteBySessionAndAvatar(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("없는 아바타는 404 로 답한다")
    void tellsApartMissingFromForbidden() {
        // 없는 것과 남의 것을 다른 코드로 답합니다. 둘 다 404 로 뭉개면 프론트가
        // 화면을 어떻게 되돌릴지 정할 수 없습니다.
        given(avatarRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(mine, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AVATAR_NOT_FOUND);
    }
}
