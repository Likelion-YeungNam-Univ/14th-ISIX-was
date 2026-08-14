package com.closr.domain.garment.service;

import com.closr.domain.avatar.BodyGridMatcher;
import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.repository.AvatarRepository;
import com.closr.domain.garment.GarmentAsset;
import com.closr.domain.garment.GarmentAssetResolver;
import com.closr.domain.garment.dto.ResponseGarmentDetailDto;
import com.closr.domain.garment.dto.ResponseGarmentDto;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.garment.dto.ResponseGarmentSizeDto;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.GarmentLikeRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // 💡 추가됨!
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의류 카탈로그 조회.
 *
 * <p>피팅룸에서 고를 수 있는 의류를 반환합니다. 등록된 의류만 피팅할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GarmentService {

    /** 사이즈는 사전순(L·M·S)이 아니라 이 순서로 내보냅니다. DB 표기는 소문자입니다(#33). */
    private static final List<String> SIZE_ORDER = List.of("s", "m", "l");

    private static final List<String> POPULAR_ORDER = List.of(
            "tshirt_basic", "shirt_over", "pants_slacks"
    );
    private static final Map<String, List<String>> BODY_TYPE_RECOMMENDATIONS = Map.of(
            "triangle", List.of("shirt_over", "dress_basic"),
            "inverted_triangle", List.of("tshirt_basic", "pants_slacks"),
            "hourglass", List.of("shirt_slim", "skirt_pencil"),
            "rectangle", List.of("shirt_over", "pants_slacks"),
            "round", List.of("tshirt_basic", "dress_basic")
    );

    private final GarmentRepository garmentRepository;
    private final GarmentSizeSpecRepository garmentSizeSpecRepository;
    private final GarmentLikeRepository garmentLikeRepository;
    private final AvatarRepository avatarRepository;
    private final GarmentAssetResolver garmentAssetResolver;
    private final BodyGridMatcher bodyGridMatcher;

    public ResponseGarmentListDto getGarmentList() {
        return getGarmentList(null, null);
    }
    public ResponseGarmentListDto getGarmentList(String sort, String bodyType) {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();

        if (bodyType != null && !bodyType.isBlank()) {
            garments = garments.stream()
                    .filter(garment -> isRecommendedForBodyType(garment, bodyType))
                    .toList();
        }

        if ("popular".equals(sort)) {
            Map<String, Integer> rankMap = IntStream.range(0, POPULAR_ORDER.size())
                    .boxed()
                    .collect(Collectors.toMap(POPULAR_ORDER::get, i -> i));

            garments = garments.stream()
                    .sorted(Comparator.comparingInt(g -> rankMap.getOrDefault(g.getDesign(), Integer.MAX_VALUE)))
                    .toList();
        }

        Map<Long, List<String>> sizesByGarmentId = findSizesByGarmentId(garments);

        List<ResponseGarmentDto> items = garments.stream()
                .map(garment -> new ResponseGarmentDto(
                        garment.getId(),
                        garment.getName(),
                        garment.getThumbnailUrl(),
                        garment.getCategory(),
                        sizesByGarmentId.getOrDefault(garment.getId(), List.of())
                ))
                .toList();

        return new ResponseGarmentListDto(items);
    }
    private boolean isRecommendedForBodyType(Garment garment, String bodyType) {
        if (bodyType == null || bodyType.isBlank()) {
            return true;
        }
        List<String> recommendedDesigns = BODY_TYPE_RECOMMENDATIONS.get(bodyType.toLowerCase());

        if (recommendedDesigns != null) {
            return recommendedDesigns.contains(garment.getDesign());
        }

        return true;
    }
    /**
     * 의류 상세를 조회합니다.
     *
     * <p>avatarId를 주면 체형 구간을 계산해 사이즈별 착용 가능 여부를 함께 내려줍니다.
     * avatarId가 없거나 아바타 치수가 없으면 모든 사이즈를 착용 가능으로 봅니다.
     */
    public ResponseGarmentDetailDto getGarmentDetail(Session session, Long garmentId, Long avatarId) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.GARMENT_NOT_FOUND));

        List<String> sizes = findSizesByGarmentId(List.of(garment))
                .getOrDefault(garment.getId(), List.of());

        boolean liked = garmentLikeRepository.existsBySessionAndGarmentId(session, garmentId);

        String bucket = resolveBucket(session, avatarId);

        List<ResponseGarmentSizeDto> sizeDtos = sizes.stream()
                .map(size -> toSizeDto(garment, size, bucket))
                .toList();

        return new ResponseGarmentDetailDto(
                garment.getId(),
                garment.getName(),
                garment.getThumbnailUrl(),
                garment.getCategory(),
                garment.getDesign(),
                garment.getFit(),
                garment.getPurchaseUrl(),
                liked,
                sizeDtos
        );
    }

    /**
     * 체형 구간을 계산합니다.
     *
     * <p>avatarId가 없으면 null을 반환합니다. 이 경우 모든 사이즈가
     * available: true로 처리됩니다.
     */
    private String resolveBucket(Session session, Long avatarId) {
        if (avatarId == null) {
            return null;
        }

        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));

        if (!avatar.getSession().getId().equals(session.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Double chestCirc = avatar.getMeasurements() != null
                ? avatar.getMeasurements().get("chest_circ") : null;

        if (avatar.getHeight() == null || chestCirc == null) {
            return null;
        }

        return bodyGridMatcher.assign(avatar.getHeight(), chestCirc);
    }

    /** 사이즈 하나의 착용 가능 여부를 판정합니다. */
    private ResponseGarmentSizeDto toSizeDto(Garment garment, String size, String bucket) {
        if (bucket == null) {
            return new ResponseGarmentSizeDto(size, true, null);
        }

        GarmentAsset asset = garmentAssetResolver.resolve(garment.getDesign(), size, bucket);
        boolean available = asset.unavailableReason() == null;
        String reason = available ? null : asset.unavailableReason().name();

        return new ResponseGarmentSizeDto(size, available, reason);
    }

    /**
     * 의류별로 선택 가능한 사이즈를 모읍니다.
     *
     * <p>스펙이 등록된 사이즈만 내보냅니다. 스펙이 없는 의류는 빈 목록이 됩니다.
     */
    private Map<Long, List<String>> findSizesByGarmentId(List<Garment> garments) {
        if (garments.isEmpty()) {
            return Map.of();
        }

        List<Long> garmentIds = garments.stream().map(Garment::getId).toList();

        return garmentSizeSpecRepository.findByGarmentIdIn(garmentIds).stream()
                .collect(Collectors.groupingBy(
                        spec -> spec.getGarment().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                specs -> specs.stream()
                                        .map(GarmentSizeSpec::getSize)
                                        .sorted(Comparator.comparingInt(SIZE_ORDER::indexOf))
                                        .toList()
                        )
                ));
    }
}