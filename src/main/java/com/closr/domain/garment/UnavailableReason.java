package com.closr.domain.garment;

/**
 * 3D 미리보기를 제공할 수 없는 이유.
 *
 * <p><b>에러가 아닙니다.</b> "안 맞습니다" 가 정답인 조합이 있어서, 미리보기만 빼고
 * 판정 결과는 그대로 내려줍니다. 값이 있으면 {@code glbUrl} 과 {@code easeUrl} 이
 * 함께 {@code null} 입니다.
 *
 * <p>별도 boolean({@code renderable} 등)을 두지 않습니다. {@code glbUrl == null} 과
 * 같은 사실을 두 번 표현하는 것이어서 두 값이 어긋나는 상태를 만들 수 있습니다.
 * 프론트는 {@code if (unavailableReason)} 한 줄로 분기합니다.
 */
public enum UnavailableReason {

    /**
     * 옷이 몸에 비해 작은 쪽에서 미리보기를 만들지 못한 조합입니다.
     * 현재 3건 있고 {@code missing_combos.json} 에 기록돼 있습니다.
     *
     * <p><b>이름이 실제 판정과 정확히 맞지는 않습니다.</b> 3건 중
     * {@code shirt_slim_m__H2B3} 는 옷 둘레가 몸보다 오히려 1.0cm
     * <b>큽니다</b>(102.0 / 101.0). 실제 실패는 시뮬레이터의 자기 관통 검사에서
     * 갈립니다. 값 이름은 프론트와의 계약이라 그대로 두고, 의미는 위 문장으로
     * 읽습니다. (2026-08-14 의류 파트 원본 대조)
     *
     * <p><b>"착용 불가" 를 뜻하지 않습니다.</b> 착용 가능 여부는
     * {@code wearable} 이 말합니다. 이 값은 미리보기 유무만 나타냅니다.
     */
    TOO_SMALL,

    /**
     * 체형 구간을 정할 수 없어 파일명을 조합할 수 없는 경우입니다.
     * 키나 가슴둘레 계측이 빠지면 발생합니다.
     *
     * <p>이 값을 지우지 마세요. 이 경로가 없으면 {@code glbUrl} 이 {@code null} 인데
     * 이유는 비어 있는 응답이 나가고, 프론트가 분기하지 못해 3D 로더가 에러를 던집니다.
     */
    SIMULATION_FAILED
}
