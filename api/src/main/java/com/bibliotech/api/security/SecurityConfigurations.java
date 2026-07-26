package com.bibliotech.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                // Avisa ao Spring que nossa API é Stateless (não guarda sessão, usaremos Token)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Libera as rotas do Swagger para podermos ler a documentação sem precisar de login
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    
                    // Libera a rota de criação de usuários (afinal, a pessoa precisa se cadastrar antes de ter token)
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios").permitAll();
                    
                    // Qualquer outra rota (livros, empréstimos) fica trancada exigindo autenticação
                    req.anyRequest().authenticated();
                })
                .build();
    }
}