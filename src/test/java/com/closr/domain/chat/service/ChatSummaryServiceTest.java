package com.closr.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.chat.dto.ResponseChatSummaryDto;
import com.closr.domain.chat.dto.ResponseChatSummaryDto.Item;
import com.closr.domain.chat.entity.ChatMode;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.user.entity.Session;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 상담 요약 조립 테스트.
 *
 * <p>지키려는 것이 넷입니다. <b>수치를 다시 계산하지 않고 그때 저장한 값을 읽는지</b>,
 * <b>본 순서대로 나오는지</b>, <b>가장 잘 맞은 옷을 고르는지</b>, 그리고
 * <b>기록이 어긋나도 요약 전체가 죽지 않는지</b>입니다.
 */
class ChatSummaryServiceTest {

    private static final Long AVATAR_ID = 32L;

    private ChatHistoryService chatHistoryService;
    private FittingRecordRepository fittingRecordRepository;
    private ChatSummaryService service;
    private Session session;
    private Avatar avatar;

    @BeforeEach
    void setUp() {
        chatHistoryService = Mockito.mock(ChatHistoryService.class);
        fittingRecordRepository = Mockito.mock(FittingRecordRepository.class);
        service = new ChatSummaryService(chatHistoryService, fittingRecordRepository);
        session = Mockito.mock(Session.class);

        avatar = Mockito.mock(Avatar.class);
        given(avatar.getId()).willReturn(AVATAR_ID);
    }

    // --- 준비물 -----------------------------------------------------------------

    /** 대화를 물으면 이 대화가 나오도록 합니다. 소유 확인은 별도 테스트에서 다룹니다. */
    private void givenConversation(Map<String, Object> summary) {
        Conversation conversation = Conversation.builder()
                .conversationId("cv_test").session(session)
                .mode(ChatMode.FITTING).avatar(avatar).build();
        if (summary != null) {
            conversation.updateSummary(summary);
        }
        given(chatHistoryService.findOwned(any(), anyString())).willReturn(conversation);
    }

    /** 조회는 최신순입니다. 인자는 오래된 것부터 주고 여기서 뒤집습니다. */
    private void givenRecords(FittingRecord... oldestFirst) {
        List<FittingRecord> newestFirst = new ArrayList<>(List.of(oldestFirst));
        java.util.Collections.reverse(newestFirst);
        given(fittingRecordRepository.findAllBySessionWithGarment(session)).willReturn(newestFirst);
    }

    private Garment garment(Long id, String name, String category) {
        Garment garment = Garment.builder()
                .design("d" + id).name(name).category(category).fit("슬림").build();
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

    private FittingRecord record(Garment garment, String recommended, boolean wearable,
                                 Map<String, Object> result) {
        return record(garment, recommended, wearable, result, avatar);
    }

    private FittingRecord record(Garment garment, String recommended, boolean wearable,
                                 Map<String, Object> result, Avatar owner) {
        return FittingRecord.builder()
                .session(session).avatar(owner).garment(garment)
                .recommendedSize(recommended).wearable(wearable).result(result).build();
    }

    /**
     * 그때 내려준 응답 전문의 모양입니다.
     *
     * <p>{@code part} 항목은 부위 · 편차 · 판정 세 개짜리로 줄였습니다. 요약이
     * 읽는 것이 그 셋뿐입니다.
     */
    private Map<String, Object> result(String size, double penalty, double totalDev,
                                       Map<String, Object>... parts) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("parts", List.of(parts));
        detail.put("penalty", penalty);
        detail.put("totalDev", totalDev);
        return Map.of("sizes", Map.of(size, detail));
    }

    private Map<String, Object> part(String name, double deviation, String verdict) {
        return Map.of("part", name, "deviation", deviation, "verdict", verdict);
    }

    // --- 수치 -------------------------------------------------------------------

    @Test
    @DisplayName("판정 수치를 그때 저장한 값에서 그대로 읽는다")
    void quotesStoredDeviation() {
        // 여기서 다시 계산하면 사용자가 피팅 화면에서 본 값과 요약이 어긋납니다.
        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), "m", false,
                result("m", 2.6, 2.6, part("shoulder_width", -2.6, "꽉 낌"))));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).singleElement()
                .extracting(Item::name, Item::size, Item::note)
                .containsExactly("슬림 셔츠", "M", "어깨가 2.6cm 부족해 당김");
    }

    @Test
    @DisplayName("받침에 맞는 조사를 붙인다")
    void picksCorrectParticle() {
        // 한쪽으로 고정하면 "가슴가" 처럼 네 부위 중 하나는 반드시 틀립니다.
        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), "m", false,
                result("m", 5.0, 5.0, part("chest_circ", -5.0, "꽉 낌"))));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).note()).startsWith("가슴이");
    }

    @Test
    @DisplayName("꽉 낀 부위가 없고 전부 적정이면 그렇게 말한다")
    void saysAllGood() {
        givenConversation(null);
        givenRecords(record(garment(1L, "오버핏 셔츠", "top"), "l", true,
                result("l", 0, 1.2, part("shoulder_width", 1.2, "적정"))));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).note()).isEqualTo("전 부위 적정");
    }

    @Test
    @DisplayName("여유 있는 부위 중 가장 큰 것을 말한다")
    void reportsLargestLoosePart() {
        givenConversation(null);
        givenRecords(record(garment(1L, "오버핏 셔츠", "top"), "l", true,
                result("l", 0, 8.0,
                        part("shoulder_width", 1.2, "여유 있음"),
                        part("chest_circ", 6.8, "여유 있음"))));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).note()).isEqualTo("가슴이 6.8cm 여유");
    }

    @Test
    @DisplayName("꽉 낀 부위가 여유 있는 부위보다 먼저다")
    void tightWinsOverLoose() {
        // 못 입는 이유가 우선입니다. 여유부터 말하면 입을 수 있는 것처럼 읽힙니다.
        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), null, false,
                result("m", 2.6, 9.4,
                        part("chest_circ", 6.8, "여유 있음"),
                        part("shoulder_width", -2.6, "꽉 낌"))));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).note()).isEqualTo("어깨가 2.6cm 부족해 당김");
    }

    // --- 사이즈 -----------------------------------------------------------------

    @Test
    @DisplayName("입을 수 있는 사이즈가 없으면 가장 가까웠던 사이즈를 설명한다")
    void fallsBackToClosestSize() {
        // "착용 가능한 사이즈가 없습니다" 만 남기면 왜 안 되는지가 사라집니다.
        givenConversation(null);
        Map<String, Object> sizes = Map.of("sizes", Map.of(
                "s", Map.of("penalty", 9.0, "totalDev", 9.0,
                        "parts", List.of(part("shoulder_width", -9.0, "꽉 낌"))),
                "l", Map.of("penalty", 1.4, "totalDev", 1.4,
                        "parts", List.of(part("shoulder_width", -1.4, "꽉 낌")))));
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), null, false, sizes));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0))
                .extracting(Item::size, Item::wearable, Item::note)
                .containsExactly("L", false, "어깨가 1.4cm 부족해 당김");
    }

    // --- 순서와 추천 -------------------------------------------------------------

    @Test
    @DisplayName("본 순서대로 내보낸다")
    void keepsChronologicalOrder() {
        // 조회는 최신순입니다. 그대로 두면 "비교했다" 가 거꾸로 읽힙니다.
        givenConversation(null);
        givenRecords(
                record(garment(1L, "슬림 셔츠", "top"), "m", true, result("m", 0, 3.0)),
                record(garment(2L, "오버핏 셔츠", "top"), "l", true, result("l", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).extracting(Item::name)
                .containsExactly("슬림 셔츠", "오버핏 셔츠");
    }

    @Test
    @DisplayName("가장 잘 맞은 옷 하나에만 추천을 붙인다")
    void marksSingleBestFit() {
        givenConversation(null);
        givenRecords(
                record(garment(1L, "슬림 셔츠", "top"), "m", true, result("m", 0, 3.0)),
                record(garment(2L, "오버핏 셔츠", "top"), "l", true, result("l", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).extracting(Item::name, Item::bestFit)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("슬림 셔츠", false),
                        org.assertj.core.groups.Tuple.tuple("오버핏 셔츠", true));
    }

    @Test
    @DisplayName("입을 수 없는 옷은 추천 대상이 아니다")
    void neverRecommendsUnwearable() {
        givenConversation(null);
        givenRecords(
                record(garment(1L, "슬림 셔츠", "top"), null, false, result("m", 0.1, 0.1)),
                record(garment(2L, "오버핏 셔츠", "top"), "l", true, result("l", 0, 9.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).bestFit()).isFalse();
        assertThat(summary.items().get(1).bestFit()).isTrue();
    }

    @Test
    @DisplayName("한 벌만 봤으면 추천을 붙이지 않는다")
    void noBestFitForSingleItem() {
        // 비교 대상이 없는데 "추천" 을 붙이면 고른 것처럼 보입니다.
        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), "m", true, result("m", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items().get(0).bestFit()).isFalse();
    }

    // --- 머리말 -----------------------------------------------------------------

    @Test
    @DisplayName("같은 종류만 봤으면 종류로 부른다")
    void namesTheCategory() {
        givenConversation(null);
        givenRecords(
                record(garment(1L, "슬림 셔츠", "top"), "m", true, result("m", 0, 3.0)),
                record(garment(2L, "오버핏 셔츠", "top"), "l", true, result("l", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.headline()).isEqualTo("상의 2벌을 비교하셨습니다.");
    }

    @Test
    @DisplayName("종류가 섞이면 옷이라고 부른다")
    void fallsBackToGenericNoun() {
        // 셔츠와 슬랙스를 함께 본 상담에 "상의 2벌" 이라고 쓸 수는 없습니다.
        givenConversation(null);
        givenRecords(
                record(garment(1L, "슬림 셔츠", "top"), "m", true, result("m", 0, 3.0)),
                record(garment(2L, "슬랙스", "bottom"), "l", true, result("l", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.headline()).isEqualTo("옷 2벌을 비교하셨습니다.");
    }

    // --- 취향 -------------------------------------------------------------------

    @Test
    @DisplayName("부위 키를 한글로 옮겨 내보낸다")
    void translatesConcernKeys() {
        // 프론트가 매핑표를 또 들고 있지 않도록 합니다.
        givenConversation(Map.of("용도", "출근", "신경쓰는부위", List.of("shoulder_width"),
                "선호핏", "오버핏", "피하는것", "붙는 옷"));
        givenRecords();

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.preference().concerns()).containsExactly("어깨");
        assertThat(summary.preference().purpose()).isEqualTo("출근");
        assertThat(summary.preference().preferredFit()).isEqualTo("오버핏");
        assertThat(summary.preference().avoid()).isEqualTo("붙는 옷");
    }

    @Test
    @DisplayName("알아낸 취향이 하나도 없으면 통째로 비운다")
    void omitsEmptyPreference() {
        // 빈 껍데기를 내려보내면 프론트가 값 네 개를 각각 확인해야 합니다.
        givenConversation(new LinkedHashMap<>(Map.of("신경쓰는부위", List.of(), "용도", "")));
        givenRecords();

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.preference()).isNull();
    }

    @Test
    @DisplayName("첫 대화라 요약이 없으면 비운다")
    void handlesMissingSummary() {
        givenConversation(null);
        givenRecords();

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.preference()).isNull();
    }

    // --- 비어 있거나 어긋난 경우 ---------------------------------------------------

    @Test
    @DisplayName("옷을 하나도 안 본 상담은 머리말이 없다")
    void emptyConsultationHasNoHeadline() {
        // 프론트가 headline · items · preference 셋으로 노출 여부를 정합니다.
        givenConversation(null);
        givenRecords();

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.headline()).isNull();
        assertThat(summary.items()).isEmpty();
    }

    @Test
    @DisplayName("다른 아바타의 피팅은 이 상담에 넣지 않는다")
    void ignoresOtherAvatars() {
        // 아바타를 다시 만들었다면 그 전 치수의 판정은 지금 상담과 무관합니다.
        Avatar other = Mockito.mock(Avatar.class);
        given(other.getId()).willReturn(99L);

        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), "m", true,
                result("m", 0, 1.0), other));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).isEmpty();
    }

    @Test
    @DisplayName("같은 옷을 여러 번 봤으면 최신 것만 남긴다")
    void keepsOnlyLatestPerGarment() {
        // 사이즈를 바꿔 가며 본 경우 같은 셔츠가 세 줄로 나옵니다.
        givenConversation(null);
        Garment slim = garment(1L, "슬림 셔츠", "top");
        givenRecords(
                record(slim, "s", false, result("s", 3.0, 3.0)),
                record(slim, "m", true, result("m", 0, 1.0)));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).singleElement()
                .extracting(Item::size).isEqualTo("M");
    }

    @Test
    @DisplayName("판정을 읽지 못해도 이름과 사이즈는 내보낸다")
    void survivesUnreadableResult() {
        // 옛 형식으로 저장된 기록입니다. 설명만 빠지고 요약은 그대로 나옵니다.
        givenConversation(null);
        givenRecords(record(garment(1L, "슬림 셔츠", "top"), "m", true, Map.of()));

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).singleElement()
                .extracting(Item::name, Item::size, Item::note)
                .containsExactly("슬림 셔츠", "M", "착용 가능");
    }

    @Test
    @DisplayName("여섯 벌 이상 봤으면 최근 다섯 벌만 담는다")
    void capsAtFive() {
        givenConversation(null);
        FittingRecord[] records = new FittingRecord[6];
        for (int i = 0; i < 6; i++) {
            records[i] = record(garment((long) i + 1, "옷" + i, "top"), "m", true,
                    result("m", 0, 1.0));
        }
        givenRecords(records);

        ResponseChatSummaryDto summary = service.findSummary(session, "cv_test");

        assertThat(summary.items()).hasSize(5)
                .extracting(Item::name).doesNotContain("옷0");
    }
}
