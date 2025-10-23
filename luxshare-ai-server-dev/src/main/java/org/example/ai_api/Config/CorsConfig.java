package org.example.ai_api.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许跨域携带凭证（如 Cookies）
        config.setAllowCredentials(false);

        // 允许的请求来源（根据你的前端地址调整）
        config.addAllowedOrigin("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");

        //暴露特定响应头
        config.addExposedHeader("Content-Disposition");
//        config.addExposedHeader("Authorization");
//        config.addExposedHeader("Access-Control-Allow-Origin");

        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}