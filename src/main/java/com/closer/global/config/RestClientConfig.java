package com.closer.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactories;
import org.springframework.web.client.RestClient;

/**
 * AI 서버 호출용 RestClient.
 *
 * <p>아바타 생성이 최대 30초 걸리므로 타임아웃을 넉넉히 잡습니다.
 * 기본값(보통 5~10초)으로 두면 정상 요청도 실패합니다.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.server.timeout}")
    private long aiServerTimeout;

    @Bean
    public RestClient aiRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofMillis(aiServerTimeout));

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
