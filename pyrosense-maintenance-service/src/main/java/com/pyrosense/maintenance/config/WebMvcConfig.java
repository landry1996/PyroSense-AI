package com.pyrosense.maintenance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AllowedFieldsInterceptor allowedFieldsInterceptor;

    public WebMvcConfig(AllowedFieldsInterceptor allowedFieldsInterceptor) {
        this.allowedFieldsInterceptor = allowedFieldsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(allowedFieldsInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
