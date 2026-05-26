package com.pyrosense.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AllowedFieldsInterceptor allowedFieldsInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(AllowedFieldsInterceptor allowedFieldsInterceptor,
                        RateLimitInterceptor rateLimitInterceptor) {
        this.allowedFieldsInterceptor = allowedFieldsInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(allowedFieldsInterceptor)
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
