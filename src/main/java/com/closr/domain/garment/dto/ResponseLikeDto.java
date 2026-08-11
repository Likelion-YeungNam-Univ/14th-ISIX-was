package com.closr.domain.garment.dto;

import com.closr.domain.garment.entity.GarmentLike;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "찜한 의류 항목")
public record ResponseLikeDto(
        @Schema(description = "의류 ID") Long garmentId,
        @Schema(description = "의류 이름") String name,
        @Schema(description = "카테고리") String category,
        @Schema(description = "썸네일 URL") String thumbnailUrl,
        @Schema(description = "찜한 시각") LocalDateTime likedAt
) {
    public static ResponseLikeDto from(GarmentLike like) {
        var g = like.getGarment();
        return new ResponseLikeDto(
                g.getId(), g.getName(), g.getCategory(),
                g.getThumbnailUrl(), like.getCreatedAt()
        );
    }
}
