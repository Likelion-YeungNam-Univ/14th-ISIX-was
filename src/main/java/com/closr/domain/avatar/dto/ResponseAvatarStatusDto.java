package com.closr.domain.avatar.dto;

import java.util.Map;

public record ResponseAvatarStatusDto(
        String status,          // "processing", "done", "failed" 등
        Long avatarId,
        String jobId,
        String glbUrl,
        Map<String, Double> measurements // 12부위 치수
) {}