package com.closr.domain.garment.dto;

public record ResponseGarmentSizeDto(
        String size,
        boolean available,
        String unavailableReason
) {}