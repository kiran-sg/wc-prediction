package com.wc.prediction.wcprediction.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Autowired
    private SuperAdminInterceptor superAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**", "/api/users", "/api/users/**", "/api/matches/sync-*")
                .excludePathPatterns("/api/users/validate");
        registry.addInterceptor(superAdminInterceptor)
                .addPathPatterns("/api/superadmin/**");
    }
}
