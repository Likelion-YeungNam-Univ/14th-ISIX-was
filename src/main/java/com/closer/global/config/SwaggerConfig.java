package com.closer.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서 설정. /docs 에서 확인합니다.
 *
 * <p>컨트롤러에는 스웨거 어노테이션을 직접 작성하지 않습니다.
 * api 패키지에 인터페이스를 만들어 명세를 관리하고, 컨트롤러가 이를 구현합니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CLOSER API")
                        .description("3D 가상 피팅 플랫폼")
                        .version("v0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
