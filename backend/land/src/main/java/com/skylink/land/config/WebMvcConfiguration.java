package com.skylink.land.config;

import com.skylink.land.auth.JwtAuthenticationInterceptor;
import com.skylink.land.auth.JwtProperties;
import com.skylink.land.auth.PermissionAuthorizationInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class WebMvcConfiguration implements WebMvcConfigurer {

    private static final long CORS_MAX_AGE_SECONDS = 3600;

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;

    private final JwtProperties jwtProperties;

    @Value("${skylink.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
    private List<String> allowedOriginPatterns;

    public WebMvcConfiguration(
        JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
        PermissionAuthorizationInterceptor permissionAuthorizationInterceptor,
        JwtProperties jwtProperties
    ) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.permissionAuthorizationInterceptor = permissionAuthorizationInterceptor;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
            .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(CORS_MAX_AGE_SECONDS);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(jwtProperties.getExcludePaths());
        registry.addInterceptor(permissionAuthorizationInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(jwtProperties.getExcludePaths());
    }
}
