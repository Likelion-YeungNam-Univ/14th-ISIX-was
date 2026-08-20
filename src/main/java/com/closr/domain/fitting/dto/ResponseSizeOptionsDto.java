package com.closr.domain.fitting.dto;

/**
 * 사이즈별 피팅 결과.
 *
 * <p><b>키는 소문자입니다.</b> R2 파일명이 대소문자를 구분해 사이즈를 소문자로 통일했고,
 * DB 와 {@code recommendedSize} 도 소문자입니다. 여기만 대문자로 두면
 * {@code sizes[recommendedSize]} 가 {@code undefined} 가 됩니다.
 *
 * <p>필드 이름이 곧 JSON 키라 {@code @JsonProperty} 를 붙이지 않습니다.
 */
public record ResponseSizeOptionsDto(
        ResponseSizeDetailDto s,
        ResponseSizeDetailDto m,
        ResponseSizeDetailDto l
) {}
