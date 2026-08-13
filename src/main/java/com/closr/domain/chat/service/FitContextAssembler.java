package com.closr.domain.chat.service;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.fitting.FitVerdict;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.fitting.service.FittingService;
import com.closr.domain.user.entity.Session;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 에 보낼 근거 뭉치({@code fit_context})를 만듭니다.
 *
 * <p><b>AI 는 계산하지 않고 인용만 합니다.</b> 두 곳에서 계산하면 화면 리포트와
 * 챗봇 답변이 다른 값을 말할 수 있습니다. 명세서 정확도 항목
 * {@code 사이즈 추천 일관성}(MUST)이 그걸 금지합니다.
 *
 * <p>필드 이름이 snake_case 입니다. AI 서버가 파이썬이라 그쪽 표기를 따릅니다.
 * PR #26 이 이 불일치로 값이 조용히 {@code null} 이 된 건이라, 변환을 여기
 * 한 곳에만 둡니다.
 *
 * <p><b>{@code body_type} 은 보내지 않습니다.</b> 격자 12구간 전부가 ±4cm 계측
 * 오차에 30% 이상 타입이 바뀌어, 챗봇이 말로 단정하면 화면 문구보다 강하게
 * 남습니다. 체형은 화면에서만 보여줍니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FitContextAssembler {

    private final FittingService fittingService;
    private final PastFittingReader pastFittingReader;

    /**
     * @param garmentId 의류를 고르지 않은 상태면 {@code null}. 그때는 치수만 보냅니다
     * @param size      사용자가 고른 사이즈. 없으면 추천 사이즈를 씁니다
     */
    public Map<String, Object> assemble(Session session, Avatar avatar,
                                        Long garmentId, String size) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("measurements", avatar.getMeasurements() == null
                ? Map.of() : avatar.getMeasurements());
        // 없으면 빈 배열입니다. null 로 두면 프롬프트의 "경고가 있으면 안내" 지시가
        // 참조할 대상을 잃습니다.
        context.put("warnings", avatar.getWarnings() == null
                ? List.of() : avatar.getWarnings());

        // 대화 요약은 별건입니다. 자리를 비워 두어야 AI 가 항상 같은 형태를 받습니다.
        context.put("profile", null);
        // 지금 보고 있는 옷은 제외합니다. 자기 자신과 비교할 수 없습니다.
        context.put("past_fittings", pastFittingReader.read(session, garmentId));

        if (garmentId == null) {
            return context;
        }

        FittingService.Evaluation evaluation =
                fittingService.evaluate(session, avatar.getId(), garmentId);
        ResponseFittingDto fitting = evaluation.response();

        String chosen = normalize(size == null ? fitting.recommendedSize() : size);
        ResponseSizeDetailDto detail = detailOf(fitting, chosen);
        if (detail == null) {
            // 사이즈 스펙에 없는 값을 받은 경우입니다. 판정 없이 치수만 보냅니다.
            log.warn("사이즈 {} 의 판정 결과가 없습니다. garmentId={}", chosen, garmentId);
            return context;
        }

        context.put("garment_id", evaluation.garment().getDesign());
        context.put("size", chosen);
        context.put("fit", evaluation.garment().getFit());
        context.put("recommended_size", fitting.recommendedSize());
        context.put("unavailable_reason", detail.unavailableReason() == null
                ? null : detail.unavailableReason().name());
        context.put("fit_report", toFitReport(detail.parts()));

        return context;
    }

    /**
     * 부위별 판정을 AI 표기로 바꿉니다.
     *
     * <p>{@code verdict} 를 영문 코드로 보냅니다. 화면용 한글 라벨("꽉 낌")을
     * 그대로 보내면 프롬프트가 부위 키와 연결하지 못합니다.
     *
     * <p>{@code color} 는 보내지 않습니다. 색은 화면이 정할 일이고, 챗봇은
     * "색으로 사이즈를 말하지 않는다" 는 규칙을 지켜야 합니다.
     */
    private List<Map<String, Object>> toFitReport(List<ResponseFitPartDto> parts) {
        List<Map<String, Object>> report = new ArrayList<>();
        for (ResponseFitPartDto part : parts) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("part", part.part());
            entry.put("actual_ease", part.actualEase());
            entry.put("ref_ease", part.refEase());
            entry.put("deviation", part.deviation());
            entry.put("verdict", codeOf(part.verdict()));
            report.add(entry);
        }
        return report;
    }

    private String codeOf(String label) {
        for (FitVerdict verdict : FitVerdict.values()) {
            if (verdict.getLabel().equals(label)) {
                return verdict.getCode();
            }
        }
        // 라벨이 바뀌었는데 여기를 안 고친 경우입니다. 조용히 넘기면 AI 가
        // 모르는 값을 받아 판정을 무시합니다.
        log.error("판정 라벨 '{}' 에 대응하는 코드가 없습니다.", label);
        return label;
    }

    private ResponseSizeDetailDto detailOf(ResponseFittingDto fitting, String size) {
        return switch (size) {
            case "s" -> fitting.sizes().s();
            case "m" -> fitting.sizes().m();
            case "l" -> fitting.sizes().l();
            default -> null;
        };
    }

    private String normalize(String size) {
        return size == null ? "" : size.toLowerCase(Locale.ROOT);
    }
}
