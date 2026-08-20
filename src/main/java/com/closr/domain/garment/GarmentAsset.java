package com.closr.domain.garment;

/**
 * 의류 한 조합의 사전 계산 결과물 주소.
 *
 * <p>{@code easeUrl} 은 히트맵 토글이 읽는 여유량 파일입니다. {@code glbUrl} 과 같은
 * 조건으로 채워지므로, 둘 중 하나만 있는 상태는 없습니다.
 */
public record GarmentAsset(
        String glbUrl,
        String easeUrl,
        UnavailableReason unavailableReason
) {

    static GarmentAsset available(String glbUrl, String easeUrl) {
        return new GarmentAsset(glbUrl, easeUrl, null);
    }

    static GarmentAsset unavailable(UnavailableReason reason) {
        return new GarmentAsset(null, null, reason);
    }
}
