package com.closr.global.config;

import com.closr.global.security.SessionTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final SessionTokenFilter sessionTokenFilter;

    @Bean
    public FilterRegistrationBean<SessionTokenFilter> sessionTokenFilterRegistration() {
        FilterRegistrationBean<SessionTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(sessionTokenFilter);
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(1);
        return registration;
    }
}