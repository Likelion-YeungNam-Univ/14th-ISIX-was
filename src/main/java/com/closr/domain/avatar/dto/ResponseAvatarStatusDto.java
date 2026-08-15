package com.closr.domain.avatar.dto;

import java.util.List;
import java.util.Map;

public record ResponseAvatarStatusDto(
        String status,
        Long avatarId,
        String jobId,
        String glbUrl,
        Map<String, Double> measurements,
        Double confidence,
        List<String> warnings,
        String bodyType,
        String bodyTypeLabel,
        String bodyTypeMessage
) {}