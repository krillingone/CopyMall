package com.krill.mall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class MallCorsConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConf = new CorsConfiguration();

        // 配置跨域
        corsConf.addAllowedHeader("*");
        corsConf.addAllowedMethod("*");
        corsConf.addAllowedOrigin("*");
        corsConf.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", corsConf);

        return new CorsWebFilter(source);
    }
}
