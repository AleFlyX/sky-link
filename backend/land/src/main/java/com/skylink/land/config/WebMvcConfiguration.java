package com.skylink.land.config;

import com.skylink.land.auth.JwtAuthenticationInterceptor;
import com.skylink.land.auth.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    private final JwtProperties jwtProperties;

    public WebMvcConfiguration(JwtAuthenticationInterceptor jwtAuthenticationInterceptor, JwtProperties jwtProperties) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(jwtProperties.getExcludePaths());
    }
}
