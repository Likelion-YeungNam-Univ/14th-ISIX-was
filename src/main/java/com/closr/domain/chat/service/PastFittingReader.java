package com.closr.domain.chat.service;

import com.closr.domain.fitting.FitVerdict;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.fitting.repository.FittingRecordRepository;
import com.closr.domain.user.entity.Session;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지난 피팅 기록을 챗봇이 인용할 형태로 바꿉니다.
 *
 * <p>비교 발화의 근거입니다 — "그때 보신 슬림 셔츠는 어깨가 꽉 꼈는데".
 * 수치를 다시 계산하지 않고 <b>그때 저장한 결과를 그대로 읽습니다.</b> 판정 기준이
 * 나중에 바뀌어도 사용자가 그때 본 값이 나와야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PastFittingReader {

    /**
     * 최대 건수. 프롬프트에 넣을 수 있는 분량이고, 답변은 2~3문장이라 더 많아도
     * 쓰이지 않습니다. 많이 넣으면 토큰만 늘고 모델이 엉뚱한 것을 고릅니다.
     */
    private static final int LIMIT = 5;

    private final FittingRecordRepository fittingRecordRepository;

    /**
     * @param currentGarmentId 지금 보고 있는 의류. 같은 옷은 제외합니다
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> read(Session session, Long currentGarmentId) {
        List<FittingRecord> records = fittingRecordRepository.findAllBySessionWithGarment(session);

        List<Map<String, Object>> past = new ArrayList<>();
        // 같은 옷을 여러 번 봤으면 최신 것만 남깁니다. 안 그러면 같은 셔츠가
        // 다섯 번 들어가고, 비교할 다른 옷이 밀려 나갑니다.
        Set<Long> seen = new LinkedHashSet<>();

        for (FittingRecord record : records) {
            Long garmentId = record.getGarment().getId();

            // 지금 보고 있는 옷과 비교하면 "이 옷은 이 옷과 같습니다" 가 됩니다.
            if (garmentId.equals(currentGarmentId) || !seen.add(garmentId)) {
                continue;
            }

            past.add(toEntry(record));
            if (past.size() >= LIMIT) {
                break;
            }
        }
        return past;
    }

    private Map<String, Object> toEntry(FittingRecord record) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("garment_id", record.getGarment().getDesign());
        entry.put("size", normalize(record.getRecommendedSize()));
        entry.put("wearable", record.isWearable());
        entry.put("tight_parts", tightParts(record));
        entry.put("recommended_size", normalize(record.getRecommendedSize()));
        return entry;
    }

    /**
     * 추천 사이즈에서 꽉 낀 부위를 뽑습니다.
     *
     * <p>{@code result} 는 그때 내려준 응답 전문입니다. 판정 라벨이 한글로
     * 저장돼 있어 {@link FitVerdict#getLabel()} 과 비교합니다. AI 에는 부위 키만
     * 보내므로 라벨을 다시 옮기지는 않습니다.
     */
    private List<String> tightParts(FittingRecord record) {
        Map<String, Object> result = record.getResult();
        if (result == null) {
            return List.of();
        }

        Object sizes = result.get("sizes");
        if (!(sizes instanceof Map<?, ?> bySize)) {
            return List.of();
        }

        Object detail = bySize.get(normalize(record.getRecommendedSize()));
        if (!(detail instanceof Map<?, ?> sizeDetail)) {
            // 기록이 대문자 키로 저장된 옛 형식일 수 있습니다. 비교 발화만
            // 빠지고 나머지는 정상 동작합니다.
            log.debug("피팅 기록 {} 에서 사이즈 {} 상세를 찾지 못했습니다.",
                    record.getId(), record.getRecommendedSize());
            return List.of();
        }

        Object parts = sizeDetail.get("parts");
        if (!(parts instanceof List<?> partList)) {
            return List.of();
        }

        List<String> tight = new ArrayList<>();
        for (Object part : partList) {
            if (part instanceof Map<?, ?> partMap
                    && FitVerdict.TIGHT.getLabel().equals(partMap.get("verdict"))) {
                Object name = partMap.get("part");
                if (name != null) {
                    tight.add(name.toString());
                }
            }
        }
        return tight;
    }

    private String normalize(String size) {
        return size == null ? "" : size.toLowerCase(Locale.ROOT);
    }
}
