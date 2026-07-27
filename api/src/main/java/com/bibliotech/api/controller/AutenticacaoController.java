package com.bibliotech.api.controller;

import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping
    public ResponseEntity efetuarLogin(@RequestBody DadosAutenticacao dados) {
        // Empacota o email e senha
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
        
        // O Spring Security vai lá no banco de dados e verifica se a senha bate
        var authentication = manager.authenticate(authenticationToken);
        
        // Se a senha bater, geramos a pulseira VIP (Token JWT)
        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());
        
        // Devolvemos o token na tela
        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }
    
    // Pequenos Records (DTOs) para estruturar a entrada e saída do JSON
    public record DadosAutenticacao(String email, String senha) {}
    public record DadosTokenJWT(String token) {}
}