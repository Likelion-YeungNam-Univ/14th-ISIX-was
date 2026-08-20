package com.closr.domain.chat.dto;

import java.util.List;

/**
 * 상담 요약 응답.
 *
 * <p>상담이 끝난 뒤 "오늘의 상담" 화면에 뿌리는 값입니다. 대화 중에 무엇을
 * 비교했고 무엇을 알아냈는지를 한 화면으로 보여줍니다.
 *
 * <p><b>새로 계산하지 않습니다.</b> 피팅 판정은 그때 저장한 결과를 그대로 읽고,
 * 취향은 챗봇이 대화에서 뽑아 둔 값을 그대로 씁니다. 여기서 다시 계산하면 사용자가
 * 화면에서 본 수치와 요약의 수치가 어긋날 수 있습니다.
 *
 * <p>모든 필드가 비어 있을 수 있습니다. 옷을 하나도 안 본 채 대화만 했거나
 * (온보딩), 챗봇이 취향을 뽑아낼 만한 발화가 없었던 경우입니다. 그때는
 * {@code headline} 과 {@code preference} 가 {@code null} 이고 {@code items} 가
 * 빈 배열이라, 프론트는 이 셋으로 화면 노출 여부를 정하면 됩니다.
 */
public record ResponseChatSummaryDto(
        String conversationId,
        String headline,
        List<Item> items,
        Preference preference
) {

    /**
     * 비교한 옷 한 벌.
     *
     * <p>{@code note} 는 그대로 화면에 찍는 문구입니다. 프론트가 조립하지 않도록
     * 완성된 형태로 내려보냅니다 — 같은 판정을 두 곳에서 문장으로 옮기면 화면마다
     * 표현이 갈립니다.
     *
     * <p>{@code size} 의 의미가 {@code wearable} 에 따라 다릅니다. 입을 수 있으면
     * <b>추천 사이즈</b>이고, 없으면 <b>그나마 가장 가까웠던 사이즈</b>입니다.
     * 어느 쪽이든 {@code note} 가 그 사이즈에 대한 설명입니다.
     */
    public record Item(
            Long garmentId,
            String name,
            String size,
            boolean wearable,
            boolean bestFit,
            String note
    ) {}

    /**
     * 대화에서 알아낸 취향.
     *
     * <p>키는 영문, 값은 화면에 그대로 찍는 한글입니다. 저장은 챗봇이 뽑은 한글 키
     * ({@code 용도} · {@code 신경쓰는부위} · {@code 선호핏} · {@code 피하는것})로
     * 되어 있지만, 프론트 계약까지 한글 키로 두지는 않습니다.
     *
     * <p>{@code concerns} 는 {@code shoulder_width} 같은 부위 키로 저장돼 있어
     * 한글로 옮겨 내보냅니다. 프론트가 매핑표를 또 들고 있지 않도록 하기 위함입니다.
     *
     * <p>항목별로 {@code null} 일 수 있습니다. 사용자가 말하지 않은 것은 챗봇이
     * 비워 두기 때문입니다.
     */
    public record Preference(
            String purpose,
            List<String> concerns,
            String preferredFit,
            String avoid
    ) {}
}
