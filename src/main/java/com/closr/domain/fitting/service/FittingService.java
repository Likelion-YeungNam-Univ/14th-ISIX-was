package com.closr.domain.fitting.service;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.fitting.FitVerdict;
import com.closr.domain.fitting.dto.ResponseFitPartDto;
import com.closr.domain.fitting.dto.ResponseFittingDto;
import com.closr.domain.fitting.dto.ResponseSizeDetailDto;
import com.closr.domain.fitting.dto.ResponseSizeOptionsDto;
import com.closr.domain.garment.entity.FitTolerance;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.FitToleranceRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가상 피팅 판정.
 *
 * <p>사이즈 추천을 실제 여유량이 아니라 <b>목표 여유 대비 편차</b>로 합니다.
 * 실제 여유만 보면 오버핏 가슴 20cm 와 슬림 20cm 를 구분할 수 없습니다.
 *
 * <pre>
 *   실제 여유 = 의류 치수 - 아바타 치수
 *   편차      = 실제 여유 - 목표 여유
 *   판정      = 편차를 핏별 허용 범위와 비교
 * </pre>
 *
 * <p>착용 가능 여부는 <b>꽉 끼는 부위가 있는지만</b> 봅니다. 헐렁한 것은 입을 수
 * 있지만 끼는 것은 못 입기 때문입니다. 추천은 착용 가능한 사이즈 중에서 고르고,
 * 전부 불가능하면 그중 덜 심한 것을 고르되 착용 불가로 표시합니다.
 *
 * <p>시뮬레이션 파트의 {@code fit_judge.py} 와 같은 규칙입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingService {

    private static final List<String> SIZE_ORDER = List.of("S", "M", "L");

    private final AvatarRepository avatarRepository;
    private final GarmentRepository garmentRepository;
    private final GarmentSizeSpecRepository garmentSizeSpecRepository;
    private final FitToleranceRepository fitToleranceRepository;
    private final FittingRecordService fittingRecordService;

    @Transactional
    public ResponseFittingDto getFitting(Session session, Long avatarId, Long garmentId) {
        Avatar avatar = loadAvatar(session, avatarId);
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.GARMENT_NOT_FOUND));

        List<GarmentSizeSpec> specs = garmentSizeSpecRepository.findByGarmentIdIn(List.of(garmentId));
        if (specs.isEmpty()) {
            log.warn("의류 {} 의 사이즈 스펙이 없습니다.", garment.getDesign());
            throw new CustomException(ErrorCode.FITTING_NOT_AVAILABLE);
        }

        Map<String, FitTolerance> tolerances = loadTolerances(garment);

        List<SizeJudgement> judgements = specs.stream()
                .sorted(Comparator.comparingInt(spec -> SIZE_ORDER.indexOf(spec.getSize())))
                .map(spec -> judge(spec, avatar.getMeasurements(), tolerances))
                .toList();

        SizeJudgement best = pickRecommended(judgements);
        ResponseFittingDto response = toResponse(garmentId, judgements, best, avatar);

        fittingRecordService.save(session, avatar, garment,
                best.size(), best.wearable(), toRecord(response));

        return response;
    }

    private Avatar loadAvatar(Session session, Long avatarId) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));

        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (avatar.getMeasurements() == null || avatar.getMeasurements().isEmpty()) {
            // 생성이 아직 끝나지 않았거나 실패한 아바타입니다.
            throw new CustomException(ErrorCode.AVATAR_NOT_READY);
        }
        return avatar;
    }

    private Map<String, FitTolerance> loadTolerances(Garment garment) {
        List<FitTolerance> found = fitToleranceRepository.findByFit(garment.getFit());
        if (found.isEmpty()) {
            log.error("의류 {} 의 핏 '{}' 에 해당하는 허용 범위가 없습니다.",
                    garment.getDesign(), garment.getFit());
            throw new CustomException(ErrorCode.FITTING_NOT_AVAILABLE);
        }
        return found.stream().collect(Collectors.toMap(FitTolerance::getPart, Function.identity()));
    }

    /**
     * 사이즈 하나를 판정합니다.
     *
     * <p>아바타에 없는 부위는 건너뜁니다. 의류가 요구하는 부위를 아바타가 갖고 있지
     * 않으면 그 부위는 판단할 근거가 없습니다.
     */
    private SizeJudgement judge(GarmentSizeSpec spec,
                                Map<String, Double> avatarMeasurements,
                                Map<String, FitTolerance> tolerances) {
        List<ResponseFitPartDto> parts = new ArrayList<>();
        double penalty = 0.0;
        double totalDeviation = 0.0;
        boolean wearable = true;

        for (Map.Entry<String, Double> entry : spec.getMeasurements().entrySet()) {
            String part = entry.getKey();
            Double avatarValue = avatarMeasurements.get(part);
            Double targetEase = spec.getTargetEase() == null ? null : spec.getTargetEase().get(part);
            FitTolerance tolerance = tolerances.get(part);

            if (avatarValue == null || targetEase == null || tolerance == null) {
                log.debug("부위 {} 판정 생략 (아바타값 {}, 목표여유 {}, 허용범위 {})",
                        part, avatarValue, targetEase, tolerance);
                continue;
            }

            double ease = round1(entry.getValue() - avatarValue);
            double deviation = round1(ease - targetEase);
            FitVerdict verdict = FitVerdict.of(deviation, tolerance);

            parts.add(new ResponseFitPartDto(
                    part, ease, targetEase, deviation, verdict.getLabel(), verdict.getColor()));

            penalty = Math.max(penalty, overflow(deviation, tolerance));
            totalDeviation += Math.abs(deviation);
            wearable = wearable && verdict != FitVerdict.TIGHT;
        }

        if (parts.isEmpty()) {
            log.warn("판정 가능한 부위가 없습니다. 아바타 계측과 의류 스펙의 부위가 겹치지 않습니다.");
            throw new CustomException(ErrorCode.FITTING_NOT_AVAILABLE);
        }

        return new SizeJudgement(spec.getSize(), spec.getModelUrl(), parts,
                round1(penalty), round1(totalDeviation), wearable);
    }

    /** 허용 범위를 얼마나 벗어났는지. 범위 안이면 0 입니다. */
    private double overflow(double deviation, FitTolerance tolerance) {
        return Math.max(Math.max(tolerance.getDevMin() - deviation,
                deviation - tolerance.getDevMax()), 0.0);
    }

    /**
     * 추천 사이즈를 고릅니다.
     *
     * <p>착용 가능한 것 중에서 고르고, 하나도 없으면 전체에서 고릅니다.
     * 허용 범위를 덜 벗어난 쪽이 우선이고, 같으면 편차 합이 작은 쪽입니다.
     */
    private SizeJudgement pickRecommended(List<SizeJudgement> judgements) {
        List<SizeJudgement> wearableOnes = judgements.stream()
                .filter(SizeJudgement::wearable)
                .toList();
        List<SizeJudgement> candidates = wearableOnes.isEmpty() ? judgements : wearableOnes;

        return candidates.stream()
                .min(Comparator.comparingDouble(SizeJudgement::penalty)
                        .thenComparingDouble(SizeJudgement::totalDeviation))
                .orElseThrow(() -> new CustomException(ErrorCode.FITTING_NOT_AVAILABLE));
    }

    private ResponseFittingDto toResponse(Long garmentId, List<SizeJudgement> judgements,
                                          SizeJudgement best, Avatar avatar) {
        Map<String, ResponseSizeDetailDto> bySize = new LinkedHashMap<>();
        for (SizeJudgement judgement : judgements) {
            bySize.put(judgement.size(), new ResponseSizeDetailDto(
                    judgement.modelUrl(),
                    judgement.parts(),
                    judgement.penalty(),
                    judgement.totalDeviation(),
                    judgement.wearable(),
                    judgement.size().equals(best.size())));
        }

        return new ResponseFittingDto(
                garmentId,
                new ResponseSizeOptionsDto(bySize.get("S"), bySize.get("M"), bySize.get("L")),
                best.size(),
                reasonFor(best, avatar));
    }

    private String reasonFor(SizeJudgement best, Avatar avatar) {
        Double chest = avatar.getMeasurements().get("chest_circ");
        String basis = chest == null ? "" : "가슴둘레 %.1fcm 기준 ".formatted(chest);

        if (!best.wearable()) {
            String tight = best.parts().stream()
                    .filter(part -> FitVerdict.TIGHT.getLabel().equals(part.verdict()))
                    .map(ResponseFitPartDto::part)
                    .collect(Collectors.joining(", "));
            return "%s모든 사이즈가 몸에 끼는 편입니다. %s 사이즈가 가장 가깝지만 %s 이(가) 꽉 낍니다."
                    .formatted(basis, best.size(), tight);
        }

        long loose = best.parts().stream()
                .filter(part -> FitVerdict.LOOSE.getLabel().equals(part.verdict()))
                .count();
        if (loose > 0) {
            return "%s%s 사이즈를 추천합니다. 여유가 있는 부위가 있지만 착용에는 문제가 없습니다."
                    .formatted(basis, best.size());
        }
        return "%s%s 사이즈가 모든 부위에서 적정합니다.".formatted(basis, best.size());
    }

    /** 기록에는 응답을 그대로 남깁니다. 나중에 판정 기준이 바뀌어도 그때 본 값이 나와야 합니다. */
    private Map<String, Object> toRecord(ResponseFittingDto response) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("garmentId", response.garmentId());
        record.put("recommendedSize", response.recommendedSize());
        record.put("recommendationReason", response.recommendationReason());
        record.put("sizes", response.sizes());
        return record;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** 사이즈 하나의 판정 결과. 추천 선택에만 쓰는 penalty · totalDeviation 을 함께 들고 있습니다. */
    private record SizeJudgement(
            String size,
            String modelUrl,
            List<ResponseFitPartDto> parts,
            double penalty,
            double totalDeviation,
            boolean wearable
    ) {}
}
