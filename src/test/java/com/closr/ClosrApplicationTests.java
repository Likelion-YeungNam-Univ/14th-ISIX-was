package com.closr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션 컨텍스트 기동 확인.
 *
 * <p>빈 설정 · 프로필 · 설정값 오타처럼 컴파일로는 걸러지지 않는 문제를 잡습니다.
 */
@SpringBootTest
class ClosrApplicationTests {

    @Test
    @DisplayName("컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }
}
