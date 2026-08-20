package com.closr.domain.avatar.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 아바타 상태 · 목록 조회 응답.
 *
 * <p>{@code height} 와 {@code weight} 는 사용자가 생성 때 입력한 값이고
 * {@code measurements} 는 사진에서 계산한 값입니다. 둘을 섞지 않습니다.
 *
 * <p>{@code bodyTypeStyling} 만 계산값이 아닌 <b>편집 문구</b>입니다
 * ({@link com.closr.domain.avatar.BodyTypeStyling}). {@code bodyType} 에서
 * 찾아 넣습니다.
 *
 * <p>체형 관련 네 필드는 <b>함께 비어 있을 수 있습니다.</b> 가슴 · 허리 ·
 * 엉덩이 중 하나라도 계측이 실패하면 판정을 못 하기 때문입니다. 아바타 자체는
 * 정상이므로 프론트는 그 영역만 숨기면 됩니다.
 */
public record ResponseAvatarStatusDto(
        String status,
        Long avatarId,
        String jobId,
        String name,
        LocalDateTime createdAt,
        Integer height,
        Integer weight,
        String glbUrl,
        Map<String, Double> measurements,
        Double confidence,
        List<String> warnings,
        String bodyType,
        String bodyTypeLabel,
        String bodyTypeMessage,
        List<String> bodyTypeStyling
) {}
