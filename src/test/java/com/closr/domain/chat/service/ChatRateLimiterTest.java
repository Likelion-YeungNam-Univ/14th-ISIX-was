package com.closr.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.closr.global.exception.CustomException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 챗봇 호출 제한 테스트.
 *
 * <p>시간을 직접 넣어 확인합니다. 실제 시계로 1시간을 기다릴 수 없고,
 * {@code Thread.sleep} 으로 줄이면 경계 조건을 못 봅니다.
 */
class ChatRateLimiterTest {

    /** 테스트가 임의로 시간을 움직이는 시계. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-08-14T10:00:00Z");

        void plus(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final MovableClock clock = new MovableClock();
    private final ChatRateLimiter limiter = new ChatRateLimiter(clock);

    private void call(long sessionId, int times) {
        for (int i = 0; i < times; i++) {
            limiter.check(sessionId);
        }
    }

    @Test
    @DisplayName("한도까지는 통과한다")
    void allowsUpToLimit() {
        assertThatCode(() -> call(1L, ChatRateLimiter.LIMIT)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한도를 넘으면 429 로 거절한다")
    void rejectsBeyondLimit() {
        call(1L, ChatRateLimiter.LIMIT);

        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("요청이 많습니다");
    }

    @Test
    @DisplayName("세션마다 따로 센다")
    void countsPerSession() {
        call(1L, ChatRateLimiter.LIMIT);

        // 남의 세션이 한도를 채워도 내 요청은 통과해야 합니다.
        assertThatCode(() -> limiter.check(2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한 시간이 지나면 다시 통과한다")
    void recoversAfterWindow() {
        call(1L, ChatRateLimiter.LIMIT);
        clock.plus(Duration.ofHours(1).plusSeconds(1));

        assertThatCode(() -> limiter.check(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("고정 시간창이 아니라 최근 한 시간을 본다")
    void usesSlidingWindow() {
        // 정시에 카운터를 비우면 경계에서 두 배가 허용됩니다.
        // 59분에 한도를 채우고 61분에 부르면, 첫 호출만 빠져 한 번만 더 됩니다.
        call(1L, ChatRateLimiter.LIMIT);
        clock.plus(Duration.ofMinutes(59));

        assertThatThrownBy(() -> limiter.check(1L)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("시간창을 벗어난 호출만큼 다시 허용한다")
    void freesSlotsOneByOne() {
        limiter.check(1L);                                  // 10:00
        clock.plus(Duration.ofMinutes(30));
        call(1L, ChatRateLimiter.LIMIT - 1);                // 10:30 — 여기서 꽉 찼습니다

        assertThatThrownBy(() -> limiter.check(1L)).isInstanceOf(CustomException.class);

        // 10:00 호출 하나가 시간창을 벗어나면 한 자리가 생깁니다.
        clock.plus(Duration.ofMinutes(31));
        assertThatCode(() -> limiter.check(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.check(1L)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("거절된 요청은 세지 않는다")
    void rejectedCallsDoNotCount() {
        // 거절을 세면 한 번 넘긴 사용자가 계속 밀려 영구히 막힙니다.
        call(1L, ChatRateLimiter.LIMIT);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> limiter.check(1L)).isInstanceOf(CustomException.class);
        }

        clock.plus(Duration.ofHours(1).plusSeconds(1));

        assertThatCode(() -> call(1L, ChatRateLimiter.LIMIT)).doesNotThrowAnyException();
    }
}
