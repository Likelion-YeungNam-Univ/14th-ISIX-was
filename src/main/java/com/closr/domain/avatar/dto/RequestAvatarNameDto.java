package com.closr.domain.avatar.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestAvatarNameDto(
        @NotBlank(message = "이름은 비어있을 수 없습니다.")
        String newName
) {}