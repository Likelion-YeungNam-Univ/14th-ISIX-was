package com.closr.domain.garment.service;

import com.closr.domain.garment.dto.ResponseGarmentDto;
import com.closr.domain.garment.dto.ResponseGarmentListDto;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentSizeSpec;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.garment.repository.GarmentSizeSpecRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    /** 사이즈는 사전순(L·M·S)이 아니라 이 순서로 내보냅니다. */
    private static final List<String> SIZE_ORDER = List.of("S", "M", "L");

    private final GarmentRepository garmentRepository;
    private final GarmentSizeSpecRepository garmentSizeSpecRepository;

    public ResponseGarmentListDto getGarmentList() {
        List<Garment> garments = garmentRepository.findAllByOrderByIdAsc();
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

    /**
     * 의류별로 선택 가능한 사이즈를 모읍니다.
     *
     * <p>스펙이 등록된 사이즈만 내보냅니다. 스펙이 없는 의류는 빈 목록이 됩니다.
     * 사이즈 스펙이 아직 확정 전이라 현재는 모두 비어 있습니다.
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
