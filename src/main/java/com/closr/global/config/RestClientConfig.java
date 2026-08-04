package com.closr.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * AI 서버 호출용 RestClient.
 *
 * <p>아바타 생성은 비동기(폴링)라 개별 호출은 모두 짧습니다.
 * 작업 등록(POST)은 즉시 202 를 반환하고, 조회(GET)도 상태만 읽습니다.
 * 다만 사진 업로드가 최대 10MB 이고 AI 서버가 무거운 파이썬 프로세스라
 * 기본값보다는 여유를 둡니다.
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
