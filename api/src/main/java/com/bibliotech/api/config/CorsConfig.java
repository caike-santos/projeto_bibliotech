package com.bibliotech.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Libera acesso para TODAS as rotas (/login, /livros, etc)
                .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500", "https://caike-santos.github.io") // Autoriza especificamente o seu Live Server e GitHub Pages
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
                .allowedHeaders("*")
                .allowCredentials(true); 
    }
}