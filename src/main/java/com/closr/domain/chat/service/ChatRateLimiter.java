package com.closr.domain.chat.service;

import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 챗봇 호출 횟수 제한.
 *
 * <p><b>과금 보호입니다.</b> AI 서버는 인증 없이 열려 있고 챗 요청 한 번이 LLM
 * 호출 두 번(답변 + 요약)입니다. 제한이 없으면 한 세션이 크레딧을 다 태울 수
 * 있습니다. 명세에서 선택 사항이 아니라고 못박은 항목입니다.
 *
 * <p><b>이 엔드포인트에만 적용합니다.</b> 아바타 생성은 무거워도 자기 CPU 만
 * 쓰고, 조회는 공짜입니다. 돈이 나가는 곳만 막습니다.
 *
 * <p>메모리에 셉니다. 재시작하면 카운터가 비는데, 해커톤 기간에는 그게 문제되지
 * 않고 Redis 를 붙이는 비용이 더 큽니다. 여러 인스턴스로 늘리면 인스턴스마다
 * 따로 세므로 그때는 저장소를 옮겨야 합니다.
 */
@Slf4j
@Component
public class ChatRateLimiter {

    /** 세션당 허용 횟수. 명세 "응답 길이 및 한도" 의 값입니다. */
    static final int LIMIT = 30;

    static final Duration WINDOW = Duration.ofHours(1);

    /**
     * 이 수를 넘으면 오래된 세션을 청소합니다.
     *
     * <p>세션은 30일 살아 있어서, 쓰고 버린 카운터가 계속 쌓이면 그대로 누수가
     * 됩니다. 요청마다 전체를 훑으면 느려지므로 커졌을 때만 훑습니다.
     */
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Clock clock;
    private final Map<Long, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public ChatRateLimiter() {
        this(Clock.systemUTC());
    }

    ChatRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 한 번 쓴 것으로 세고, 한도를 넘으면 거절합니다.
     *
     * <p><b>스트림을 열기 전에 불러야 합니다.</b> 응답 헤더가 나간 뒤에는 429 로
     * 알릴 수 없어 SSE 안으로만 내려가고, 프론트가 같은 실패를 두 곳에서
     * 처리해야 합니다.
     *
     * <p>고정 시간창이 아니라 최근 1시간을 봅니다. 정시에 카운터가 비면 경계에서
     * 두 배가 허용됩니다.
     */
    public void check(Long sessionId) {
        Instant now = clock.instant();
        Instant since = now.minus(WINDOW);

        if (calls.size() > CLEANUP_THRESHOLD) {
            cleanUp(since);
        }

        Deque<Instant> recent = calls.computeIfAbsent(sessionId, key -> new ArrayDeque<>());

        // 같은 세션의 동시 요청이 카운터를 나눠 쓰지 않도록 잠급니다.
        // 세션 단위라 경합이 사실상 없고, 전역 잠금보다 낫습니다.
        synchronized (recent) {
            while (!recent.isEmpty() && recent.peekFirst().isBefore(since)) {
                recent.pollFirst();
            }

            if (recent.size() >= LIMIT) {
                log.warn("세션 {} 이 챗봇 한도를 넘었습니다 ({}회/{}).",
                        sessionId, recent.size(), WINDOW);
                throw new CustomException(ErrorCode.CHAT_RATE_LIMITED);
            }

            recent.addLast(now);
        }
    }

    /** 시간창 안에 아무 호출도 없는 세션을 지웁니다. */
    private void cleanUp(Instant since) {
        Iterator<Map.Entry<Long, Deque<Instant>>> iterator = calls.entrySet().iterator();
        int removed = 0;

        while (iterator.hasNext()) {
            Deque<Instant> recent = iterator.next().getValue();
            synchronized (recent) {
                if (recent.isEmpty() || recent.peekLast().isBefore(since)) {
                    iterator.remove();
                    removed++;
                }
            }
        }
        log.debug("챗봇 카운터 {}건을 청소했습니다. 남은 세션 {}개", removed, calls.size());
    }
}
