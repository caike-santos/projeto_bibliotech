package com.bibliotech.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {

                    req.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
                    // Libera as rotas do Swagger para podermos ler a documentação sem precisar de login
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    
                    // Libera a rota de criação de usuários (afinal, a pessoa precisa se cadastrar antes de ter token)
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios").permitAll();

                    // NOVO: Libera a rota de login para qualquer pessoa tentar entrar
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/login").permitAll();
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/login/google").permitAll();
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/login/logout").permitAll();
                    
                    // Restrições baseadas em ROLE (ADMIN e BIBLIOTECARIO) para o painel de controle
                    req.requestMatchers(org.springframework.http.HttpMethod.POST, "/livros/**").hasAnyRole("ADMIN", "BIBLIOTECARIO");
                    req.requestMatchers(org.springframework.http.HttpMethod.PUT, "/livros/**").hasAnyRole("ADMIN", "BIBLIOTECARIO");
                    req.requestMatchers(org.springframework.http.HttpMethod.DELETE, "/livros/**").hasAnyRole("ADMIN", "BIBLIOTECARIO");
                    req.requestMatchers(org.springframework.http.HttpMethod.GET, "/usuarios").hasAnyRole("ADMIN", "BIBLIOTECARIO");
                    req.requestMatchers(org.springframework.http.HttpMethod.GET, "/emprestimos").hasAnyRole("ADMIN", "BIBLIOTECARIO");
                    req.requestMatchers(org.springframework.http.HttpMethod.GET, "/reservas").hasAnyRole("ADMIN", "BIBLIOTECARIO");

                    // Qualquer outra rota (livros, empréstimos) fica trancada exigindo autenticação
                    req.anyRequest().authenticated();
                })
                // NOVO: Adiciona o nosso filtro antes do filtro padrão do Spring
                .addFilterBefore(securityFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList("*"));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Alteramos para "*" para aceitar qualquer cabeçalho que o navegador mandar no Preflight
        configuration.setAllowedHeaders(java.util.Arrays.asList("*")); 
        
        // Habilita o envio de cookies para origens cruzadas
        configuration.setAllowCredentials(true);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}