package com.closr.domain.chat.service;

import com.closr.domain.chat.dto.ResponseChatSummaryDto;
import com.closr.domain.chat.dto.ResponseChatSummaryDto.Item;
import com.closr.domain.chat.dto.ResponseChatSummaryDto.Preference;
import com.closr.domain.chat.entity.Conversation;
import com.closr.domain.fitting.FitVerdict;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.user.entity.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상담 요약 조립.
 *
 * <p>상담이 끝난 뒤 보여주는 "오늘의 상담" 을 만듭니다.
 *
 * <pre>
 *   오늘의 상담
 *
 *   상의 2벌을 비교하셨습니다.
 *   · 슬림 셔츠 M — 어깨가 2.6cm 부족해 당김
 *   · 오버핏 셔츠 L — 전 부위 적정        ← 추천
 *
 *   출근용 · 오버핏 선호 · 어깨가 신경 쓰임
 * </pre>
 *
 * <p><b>LLM 을 부르지 않습니다.</b> 재료가 전부 DB 에 있습니다 — 피팅 판정은
 * {@code fitting_records.result}, 취향은 {@code conversations.summary} 입니다.
 * 요약은 사실 나열이라 계산 쪽이고, 여기에 모델을 끼우면 같은 기록에서 매번 다른
 * 문장이 나오는 데다 틀린 수치를 걸러낼 방법이 없습니다. 챗봇 답변이 판정 수치를
 * 인용만 하도록 만든 것과 같은 이유입니다.
 *
 * <p>대화에서 취향을 <b>뽑아내는</b> 일은 AI 서버가 합니다(챗 응답의 {@code summary}).
 * 이 클래스는 그렇게 저장된 값을 <b>화면용으로 조립</b>하기만 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {

    /**
     * 요약에 담는 최대 벌 수.
     *
     * <p>한 화면에 들어가는 분량입니다. {@link PastFittingReader} 와 같은 값을
     * 쓰는데, 챗봇이 참고한 범위와 화면에 보이는 범위가 어긋나면 "요약에 없는 옷을
     * 챗봇이 말한다" 는 상황이 생깁니다.
     */
    private static final int LIMIT = 5;

    /** 부위 키를 화면 문구로 옮깁니다. AI 프롬프트의 부위 라벨과 같은 표기입니다. */
    private static final Map<String, String> PART_LABELS = Map.of(
            "shoulder_width", "어깨",
            "chest_circ", "가슴",
            "waist_circ", "허리",
            "hip_circ", "엉덩이"
    );

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "top", "상의",
            "bottom", "하의",
            "dress", "원피스"
    );

    private static final List<String> SIZE_ORDER = List.of("s", "m", "l");

    private final ChatHistoryService chatHistoryService;
    private final FittingRecordRepository fittingRecordRepository;

    /**
     * 대화 하나의 상담 요약.
     *
     * <p>남의 대화면 404 입니다. 소유 확인은 {@link ChatHistoryService#findOwned}
     * 가 합니다.
     */
    @Transactional(readOnly = true)
    public ResponseChatSummaryDto findSummary(Session session, String conversationId) {
        Conversation conversation = chatHistoryService.findOwned(session, conversationId);

        List<FittingRecord> records = collectRecords(session, conversation);
        List<Item> items = toItems(records);

        return new ResponseChatSummaryDto(
                conversation.getConversationId(),
                headline(records),
                items,
                toPreference(conversation.getSummary())
        );
    }

    /**
     * 이 상담에서 다룬 피팅 기록을 오래된 순으로 모읍니다.
     *
     * <p><b>대화 시작 시각으로 자르지 않습니다.</b> 실제 흐름이 "옷을 입혀보고 나서
     * 물어본다" 라, 첫 번째 옷의 피팅은 대화보다 먼저 생깁니다. 시각으로 자르면
     * 정작 상담의 출발점이 된 옷이 요약에서 빠집니다.
     *
     * <p>대신 <b>아바타로 좁힙니다.</b> 세션 하나가 한 번의 방문이고, 아바타를 다시
     * 만들었다면 그 전 치수의 판정은 지금 상담과 무관합니다. 온보딩 대화는 아바타가
     * 없어 결과가 비어 있습니다.
     */
    private List<FittingRecord> collectRecords(Session session, Conversation conversation) {
        if (conversation.getAvatar() == null) {
            return List.of();
        }
        Long avatarId = conversation.getAvatar().getId();

        List<FittingRecord> picked = new ArrayList<>();
        // 같은 옷을 여러 번 봤으면 최신 것만 남깁니다. 사이즈를 바꿔 가며 본 경우
        // 같은 셔츠가 세 줄로 나옵니다.
        Set<Long> seen = new LinkedHashSet<>();

        for (FittingRecord record : fittingRecordRepository.findAllBySessionWithGarment(session)) {
            if (!avatarId.equals(record.getAvatar().getId())) {
                continue;
            }
            if (!seen.add(record.getGarment().getId())) {
                continue;
            }
            picked.add(record);
            if (picked.size() >= LIMIT) {
                break;
            }
        }

        // 조회는 최신순입니다. 화면에는 본 순서대로 놓아야 "비교했다" 로 읽힙니다.
        Collections.reverse(picked);
        return picked;
    }

    private List<Item> toItems(List<FittingRecord> records) {
        List<Item> items = new ArrayList<>();
        for (FittingRecord record : records) {
            items.add(toItem(record));
        }
        markBestFit(records, items);
        return items;
    }

    private Item toItem(FittingRecord record) {
        String size = focusSize(record);
        Map<String, Object> detail = sizeDetail(record, size);

        return new Item(
                record.getGarment().getId(),
                record.getGarment().getName(),
                size == null ? null : size.toUpperCase(Locale.ROOT),
                record.isWearable(),
                false,
                note(record, detail)
        );
    }

    /**
     * 요약에서 설명할 사이즈.
     *
     * <p>입을 수 있으면 추천 사이즈입니다. 없으면 <b>가장 가까웠던 사이즈</b>를
     * 고릅니다 — "착용 가능한 사이즈가 없습니다" 만 남기면 왜 안 되는지가 사라져서,
     * 사용자가 요약을 보고도 다음에 무엇을 볼지 판단할 수 없습니다.
     */
    private String focusSize(FittingRecord record) {
        String recommended = normalize(record.getRecommendedSize());
        if (recommended != null) {
            return recommended;
        }

        String best = null;
        double bestPenalty = Double.MAX_VALUE;
        for (String size : SIZE_ORDER) {
            Map<String, Object> detail = sizeDetail(record, size);
            if (detail == null) {
                continue;
            }
            double penalty = asDouble(detail.get("penalty"), Double.MAX_VALUE);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                best = size;
            }
        }
        return best;
    }

    /**
     * 한 줄 설명을 만듭니다.
     *
     * <p>화면 목록에 붙는 짧은 구라 명사형으로 끝냅니다.
     *
     * <pre>
     *   어깨가 2.6cm 부족해 당김
     *   전 부위 적정
     *   가슴이 5.2cm 여유
     * </pre>
     *
     * <p>가장 두드러진 부위 하나만 말합니다. 네 부위를 다 적으면 목록이 아니라
     * 리포트가 되고, 그건 피팅 화면이 이미 하고 있습니다.
     */
    private String note(FittingRecord record, Map<String, Object> detail) {
        List<Map<String, Object>> parts = parts(detail);
        if (parts.isEmpty()) {
            // 옛 형식으로 저장된 기록입니다. 이름과 사이즈만 나오고 설명이 빠집니다.
            log.debug("피팅 기록 {} 에서 부위 판정을 읽지 못했습니다.", record.getId());
            return record.isWearable() ? "착용 가능" : "착용 어려움";
        }

        Map<String, Object> worstTight = worstBy(parts, FitVerdict.TIGHT);
        if (worstTight != null) {
            return "%s %scm 부족해 당김"
                    .formatted(subject(partLabel(worstTight)), centimeters(worstTight));
        }

        Map<String, Object> worstLoose = worstBy(parts, FitVerdict.LOOSE);
        if (worstLoose != null) {
            return "%s %scm 여유"
                    .formatted(subject(partLabel(worstLoose)), centimeters(worstLoose));
        }
        return "전 부위 적정";
    }

    /** 해당 판정을 받은 부위 중 편차 절대값이 가장 큰 것. 없으면 null. */
    private Map<String, Object> worstBy(List<Map<String, Object>> parts, FitVerdict verdict) {
        Map<String, Object> worst = null;
        double worstDeviation = -1;

        for (Map<String, Object> part : parts) {
            if (!verdict.getLabel().equals(part.get("verdict"))) {
                continue;
            }
            double deviation = Math.abs(asDouble(part.get("deviation"), 0));
            if (deviation > worstDeviation) {
                worstDeviation = deviation;
                worst = part;
            }
        }
        return worst;
    }

    /**
     * 가장 잘 맞은 옷 하나를 골라 표시합니다.
     *
     * <p>피팅 화면이 <b>한 벌 안에서</b> 사이즈를 고르는 기준(허용 범위를 벗어난
     * 정도 → 편차 합)을 <b>벌 사이</b>에 그대로 적용합니다. 같은 척도라 비교됩니다.
     *
     * <p>한 벌만 봤으면 표시하지 않습니다. 비교 대상이 없는데 "추천" 을 붙이면
     * 고른 것처럼 보입니다.
     */
    private void markBestFit(List<FittingRecord> records, List<Item> items) {
        if (items.size() < 2) {
            return;
        }

        int bestIndex = -1;
        double bestPenalty = Double.MAX_VALUE;
        double bestTotalDev = Double.MAX_VALUE;

        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).wearable()) {
                continue;
            }
            Map<String, Object> detail = sizeDetail(records.get(i), focusSize(records.get(i)));
            if (detail == null) {
                continue;
            }
            double penalty = asDouble(detail.get("penalty"), Double.MAX_VALUE);
            double totalDev = asDouble(detail.get("totalDev"), Double.MAX_VALUE);

            if (penalty < bestPenalty || (penalty == bestPenalty && totalDev < bestTotalDev)) {
                bestPenalty = penalty;
                bestTotalDev = totalDev;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            Item best = items.get(bestIndex);
            items.set(bestIndex, new Item(best.garmentId(), best.name(), best.size(),
                    best.wearable(), true, best.note()));
        }
    }

    /**
     * 목록 위에 붙는 한 줄.
     *
     * <p>같은 종류만 봤으면 종류로 부릅니다("상의 2벌"). 섞여 있으면 "옷" 입니다 —
     * 셔츠와 슬랙스를 함께 본 상담에 "상의 2벌" 이라고 쓸 수는 없습니다.
     */
    private String headline(List<FittingRecord> records) {
        if (records.isEmpty()) {
            return null;
        }

        Set<String> categories = new LinkedHashSet<>();
        for (FittingRecord record : records) {
            categories.add(record.getGarment().getCategory());
        }

        String noun = categories.size() == 1
                ? CATEGORY_LABELS.getOrDefault(categories.iterator().next(), "옷")
                : "옷";

        return records.size() == 1
                ? "%s 1벌을 살펴보셨습니다.".formatted(noun)
                : "%s %d벌을 비교하셨습니다.".formatted(noun, records.size());
    }

    /**
     * 저장된 취향을 화면용으로 옮깁니다.
     *
     * <p>항목이 하나도 없으면 {@code null} 입니다. 빈 껍데기를 내려보내면 프론트가
     * 값 네 개를 각각 확인해야 합니다.
     */
    private Preference toPreference(Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return null;
        }

        String purpose = text(summary.get("용도"));
        String preferredFit = text(summary.get("선호핏"));
        String avoid = text(summary.get("피하는것"));
        List<String> concerns = concerns(summary.get("신경쓰는부위"));

        if (purpose == null && preferredFit == null && avoid == null && concerns.isEmpty()) {
            return null;
        }
        return new Preference(purpose, concerns, preferredFit, avoid);
    }

    private List<String> concerns(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }

        List<String> labels = new ArrayList<>();
        for (Object part : list) {
            if (part == null) {
                continue;
            }
            // 매핑에 없는 키가 오면 키를 그대로 둡니다. 화면이 비는 것보다 낫습니다.
            labels.add(PART_LABELS.getOrDefault(part.toString(), part.toString()));
        }
        return labels;
    }

    // --- 저장된 JSON 을 읽는 잡일 -------------------------------------------------
    //
    // fitting_records.result 는 그때 내려준 응답 전문입니다. 판정 스펙이 바뀌어도
    // 사용자가 본 값을 그대로 보여주려고 통째로 담아 둔 것이라, 여기서는 형식이
    // 다를 수 있다는 전제로 읽습니다. 하나가 어긋나도 요약 전체가 실패하지 않도록
    // 전부 기본값으로 빠집니다.

    @SuppressWarnings("unchecked")
    private Map<String, Object> sizeDetail(FittingRecord record, String size) {
        if (size == null || record.getResult() == null) {
            return null;
        }
        if (!(record.getResult().get("sizes") instanceof Map<?, ?> bySize)) {
            return null;
        }
        Object detail = bySize.get(size);
        return detail instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parts(Map<String, Object> detail) {
        if (detail == null || !(detail.get("parts") instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        for (Object part : list) {
            if (part instanceof Map<?, ?> map) {
                parts.add((Map<String, Object>) map);
            }
        }
        return parts;
    }

    private String partLabel(Map<String, Object> part) {
        Object name = part.get("part");
        String key = name == null ? "" : name.toString();
        return PART_LABELS.getOrDefault(key, key);
    }

    /**
     * 부위 이름에 주격 조사를 붙입니다.
     *
     * <p>받침이 있으면 "이", 없으면 "가" 입니다 — 가슴<b>이</b> · 어깨<b>가</b>.
     * 한쪽으로 고정하면 네 부위 중 하나는 반드시 틀립니다.
     */
    private String subject(String noun) {
        if (noun.isEmpty()) {
            return noun;
        }

        char last = noun.charAt(noun.length() - 1);
        boolean hangul = last >= 0xAC00 && last <= 0xD7A3;
        if (!hangul) {
            // 매핑에 없는 키가 그대로 올라온 경우입니다(shoulder_width 등).
            return noun + "가";
        }
        boolean hasFinalConsonant = (last - 0xAC00) % 28 != 0;
        return noun + (hasFinalConsonant ? "이" : "가");
    }

    /** 편차 절대값을 소수 한 자리로. 부호는 문구가 이미 말해 줍니다. */
    private String centimeters(Map<String, Object> part) {
        double deviation = Math.abs(asDouble(part.get("deviation"), 0));
        return "%.1f".formatted(deviation);
    }

    /** jsonb 는 정수를 Integer 로, 실수를 Double 이나 BigDecimal 로 돌려줍니다. */
    private double asDouble(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String normalize(String size) {
        if (size == null || size.isBlank()) {
            return null;
        }
        return size.toLowerCase(Locale.ROOT);
    }
}
