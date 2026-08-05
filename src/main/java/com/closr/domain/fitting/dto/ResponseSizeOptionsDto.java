// ResponseSizeOptionsDto.java
package com.closr.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseSizeOptionsDto(
        @JsonProperty("S") ResponseSizeDetailDto s,
        @JsonProperty("M") ResponseSizeDetailDto m,
        @JsonProperty("L") ResponseSizeDetailDto l
) {}