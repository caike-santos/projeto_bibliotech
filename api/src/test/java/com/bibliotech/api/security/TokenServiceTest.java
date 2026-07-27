package com.bibliotech.api.security;

import com.bibliotech.api.model.Usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("Deveria gerar um token JWT válido quando o usuário for informado")
    void gerartokenenario01() {
        // Arrange (Cenário: criamos um usuário simulado)
        Usuario usuario = new Usuario();
        usuario.setEmail("caike@email.com");

        // Act (Ação: geramos o token)
        String token = tokenService.gerarToken(usuario);

        // Assert (Validação: o token não deve ser nulo)
        assertThat(token).isNotNull();
    }
}