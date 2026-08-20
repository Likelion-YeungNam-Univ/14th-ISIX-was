package com.closr.domain.garment.dto;

import java.util.List;

public record ResponseGarmentDto(
        Long garmentId,
        String name,
        String thumbnailUrl,
        String category,
        List<String> sizes
) {}
