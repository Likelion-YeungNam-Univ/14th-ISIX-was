package com.closr.domain.avatar;

import java.util.List;
import java.util.Map;

/**
 * 체형별 스타일링 안내 문구.
 *
 * <p><b>계산값이 아니라 편집 문구입니다.</b> 아바타 응답에 함께 실어 보내지만
 * 치수에서 뽑아낸 값이 아니라 사람이 쓴 안내입니다. 그래서 퍼센트 같은 수치
 * 형태를 쓰지 않습니다 — 근거 없는 숫자로 보이면 판정 수치까지 의심받습니다.
 *
 * <p>문구는 AI 파트의 체형 진단 메시지({@code bodytype.py} 의 MESSAGES)와 같은
 * 방향으로 씁니다. 같은 화면에 두 글이 나란히 놓이는데 서로 다른 말을 하면
 * 사용자가 어느 쪽을 믿을지 알 수 없습니다.
 *
 * <p><b>지금 카탈로그에 있는 6종만 언급합니다.</b> "A라인 스커트" 처럼 없는
 * 품목을 권하면 사용자가 찾아도 나오지 않습니다. 품목을 늘리면 문구도 함께
 * 늘려야 합니다.
 *
 * <p>프론트에 하드코딩하지 않고 서버에 두는 이유는 5개 체형의 문구가 한곳에
 * 모여야 서로 어긋나지 않기 때문입니다.
 */
public final class BodyTypeStyling {

    private static final Map<String, List<String>> TIPS = Map.of(
            "hourglass", List.of(
                    "허리선이 드러나는 원피스",
                    "펜슬 스커트로 허리에서 엉덩이로 이어지는 선 살리기",
                    "슬림 셔츠를 넣어 입어 허리 위치 만들기"),
            "triangle", List.of(
                    "하의는 한 사이즈 크게 — 엉덩이 여유를 먼저 확인",
                    "오버핏 셔츠로 상체에 무게 주기",
                    "몸에 붙지 않고 떨어지는 슬랙스"),
            "inverted_triangle", List.of(
                    "상의는 어깨 여유를 먼저 확인",
                    "어깨가 조이지 않는 여유 있는 상의",
                    "펜슬 스커트로 시선을 아래로 나누기"),
            "rectangle", List.of(
                    "직선적인 실루엣이 잘 맞습니다",
                    "오버핏 셔츠로 편안한 라인 만들기",
                    "슬랙스로 세로선 이어주기"),
            "round", List.of(
                    "허리를 조이지 않는 상의",
                    "오버핏 셔츠처럼 여유 있는 상의",
                    "허리 밴드가 편한 슬랙스")
    );

    private BodyTypeStyling() {
    }

    /**
     * 해당 체형의 안내 문구. 체형을 못 정했으면 빈 목록입니다.
     *
     * <p>계측이 실패하면 {@code bodyType} 이 {@code null} 로 오는데, 그때
     * 예외를 던지면 아바타 자체를 못 열게 됩니다. 화면에서 이 영역만 비면
     * 됩니다.
     */
    public static List<String> of(String bodyType) {
        return bodyType == null ? List.of() : TIPS.getOrDefault(bodyType, List.of());
    }
}
