package com.closer.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** BaseTimeEntity 의 생성일 · 수정일 자동 기록을 활성화합니다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
