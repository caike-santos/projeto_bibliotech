package com.bibliotech.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> customCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permite o envio de cookies e cabeçalhos de autenticação
        config.setAllowCredentials(true);
        
        // Lista as origens exatas para evitar problemas com padrões dinâmicos (*)
        config.setAllowedOrigins(Arrays.asList(
            "https://caike-santos.github.io",
            "http://localhost:5500",
            "http://127.0.0.1:5500"
        ));
        
        // Permite todos os cabeçalhos e métodos HTTP
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        source.registerCorsConfiguration("/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        
        // Define a ordem MAIS ALTA possível para o filtro rodar ANTES da segurança
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
