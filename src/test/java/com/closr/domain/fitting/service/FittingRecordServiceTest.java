package com.closr.domain.fitting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 피팅 기록 목록 · 삭제 테스트.
 *
 * <p>기록은 피팅을 조회할 때마다 쌓입니다. 그래서 <b>같은 옷을 두 번 보면 두 건</b>이
 * 되고, 사용자에게는 "입어본 옷이 전부 뜬다" 로 보입니다. 목록이 옷마다 최신 1건만
 * 보여주는지, 그리고 삭제가 그 화면과 어긋나지 않는지를 지킵니다.
 */
class FittingRecordServiceTest {

    private FittingRecordRepository repository;
    private FittingRecordService service;
    private Session session;
    private Avatar avatar;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FittingRecordRepository.class);
        service = new FittingRecordService(repository);
        session = Mockito.mock(Session.class);
        given(session.getId()).willReturn(1L);
        avatar = Mockito.mock(Avatar.class);
        given(avatar.getId()).willReturn(32L);
    }

    private Garment garment(Long id, String name) {
        Garment garment = Garment.builder()
                .design("d" + id).name(name).category("top").fit("슬림").build();
        setId(garment, id);
        return garment;
    }

    /** id 는 DB 가 넣는 값이라 테스트에서는 리플렉션으로 채웁니다. */
    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private FittingRecord record(Long id, Garment garment, String size) {
        FittingRecord record = FittingRecord.builder()
                .session(session).avatar(avatar).garment(garment)
                .recommendedSize(size).wearable(true).result(Map.of())
                .build();
        setId(record, id);
        return record;
    }

    @Test
    @DisplayName("같은 옷을 여러 번 봤으면 최신 1건만 보여준다")
    void keepsOnlyLatestPerGarment() {
        // 조회가 최신순이므로 먼저 만난 것이 그 옷의 최신 기록입니다.
        Garment slim = garment(2L, "슬림 셔츠");
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(30L, slim, "l"),
                record(20L, slim, "m"),
                record(10L, garment(3L, "오버핏 셔츠"), "m")));

        ResponseFittingRecordListDto result = service.findMine(session);

        assertThat(result.fittings()).hasSize(2);
        assertThat(result.fittings().get(0).fittingId()).isEqualTo(30L);
        assertThat(result.fittings()).extracting("garmentName")
                .containsExactly("슬림 셔츠", "오버핏 셔츠");
    }

    @Test
    @DisplayName("옷이 다르면 모두 보여준다")
    void keepsEveryDistinctGarment() {
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of(
                record(30L, garment(2L, "슬림 셔츠"), "m"),
                record(20L, garment(3L, "오버핏 셔츠"), "m"),
                record(10L, garment(5L, "슬랙스"), "l")));

        assertThat(service.findMine(session).fittings()).hasSize(3);
    }

    @Test
    @DisplayName("기록이 없으면 빈 목록이다")
    void emptyWhenNothingSaved() {
        given(repository.findAllBySessionWithGarment(session)).willReturn(List.of());

        assertThat(service.findMine(session).fittings()).isEmpty();
    }

    @Test
    @DisplayName("삭제는 같은 옷의 기록을 함께 지운다")
    void deleteRemovesEveryRecordOfTheSameGarment() {
        // 목록이 최신 1건만 보여주므로, 하나만 지우면 예전 기록이 그 자리에 다시
        // 올라와 지워지지 않은 것처럼 보입니다.
        Garment slim = garment(2L, "슬림 셔츠");
        given(repository.findById(30L)).willReturn(Optional.of(record(30L, slim, "l")));

        service.delete(session, 30L);

        Mockito.verify(repository).deleteBySessionAndAvatarAndGarment(session, avatar, slim);
    }

    @Test
    @DisplayName("없는 기록을 지우려 하면 404 를 던진다")
    void deleteMissingRecordThrows() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(session, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("피팅 기록");
    }

    @Test
    @DisplayName("남의 기록은 지우지 못하고 404 로 감춘다")
    void deleteOtherSessionRecordThrows() {
        // 403 은 "그 기록이 있다" 를 알려줍니다.
        Session other = Mockito.mock(Session.class);
        given(other.getId()).willReturn(99L);
        FittingRecord mine = FittingRecord.builder()
                .session(other).avatar(avatar).garment(garment(2L, "슬림 셔츠"))
                .recommendedSize("m").wearable(true).result(Map.of()).build();
        setId(mine, 30L);
        given(repository.findById(30L)).willReturn(Optional.of(mine));

        assertThatThrownBy(() -> service.delete(session, 30L))
                .isInstanceOf(CustomException.class);
        Mockito.verify(repository, Mockito.never())
                .deleteBySessionAndAvatarAndGarment(any(), any(), any());
    }
}
