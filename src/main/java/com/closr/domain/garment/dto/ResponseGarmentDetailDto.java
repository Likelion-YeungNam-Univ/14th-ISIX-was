package com.closr.domain.garment.dto;

import java.util.List;

public record ResponseGarmentDetailDto(
        Long garmentId,
        String name,
        String thumbnailUrl,
        String category,
        String design,
        String fit,
        String purchaseUrl,
        boolean liked,
        List<ResponseGarmentSizeDto> sizes
) {}