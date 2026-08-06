package com.closr.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서 설정. /docs 에서 확인합니다.
 *
 * <p>컨트롤러에는 스웨거 어노테이션을 직접 작성하지 않습니다.
 * api 패키지에 인터페이스를 만들어 명세를 관리하고, 컨트롤러가 이를 구현합니다.
 *
 * <p>회원 가입 · 로그인 없이 게스트 세션만 사용하므로 JWT Bearer 인증 설정을
 * 두지 않습니다. 세션 토큰 전달 방식이 정해지면 그때 맞춰 추가합니다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CLOSR API")
                        .description("3D 가상 피팅 플랫폼")
                        .version("v0.1.0"));
    }
}
