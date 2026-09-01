package com.bibliotech.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.bibliotech.api.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    // Puxa a senha secreta que você acabou de colocar no .env
    @Value("${JWT_SECRET:minha-senha-secreta-muito-segura-123456789}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("API BiblioTech") // Quem está emitindo
                    .withSubject(usuario.getEmail()) // O dono do token
                    .withExpiresAt(dataExpiracao()) // Quando vence (2 horas)
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    // Regra: O Token só dura 2 horas. Depois, o usuário tem que logar de novo.
    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    public String getSubject(String tokenJWT) {
        try {
            com.auth0.jwt.algorithms.Algorithm algoritmo = com.auth0.jwt.algorithms.Algorithm.HMAC256(secret);
            return com.auth0.jwt.JWT.require(algoritmo)
                    .withIssuer("API BiblioTech")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (com.auth0.jwt.exceptions.JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }
}