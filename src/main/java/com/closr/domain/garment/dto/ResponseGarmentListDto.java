package com.closr.domain.garment.dto;

import java.util.List;

public record ResponseGarmentListDto(
        List<ResponseGarmentDto> garments
) {}