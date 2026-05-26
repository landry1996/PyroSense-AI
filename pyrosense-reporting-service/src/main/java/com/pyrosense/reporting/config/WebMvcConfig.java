package com.pyrosense.reporting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AllowedFieldsInterceptor allowedFieldsInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        AllowedFieldsInterceptor allowedFieldsInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.allowedFieldsInterceptor = allowedFieldsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(allowedFieldsInterceptor)
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/reports/**");
    }

}
