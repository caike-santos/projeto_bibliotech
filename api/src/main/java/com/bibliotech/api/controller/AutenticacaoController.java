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
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bibliotech.api.repository.UsuarioRepository;
import com.bibliotech.api.repository.NotificacaoRepository;
import com.bibliotech.api.model.Notificacao;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.util.Collections;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @PostMapping
    public ResponseEntity efetuarLogin(@RequestBody DadosAutenticacao dados) {
        // Empacota o email e senha
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
        
        // O Spring Security vai lÃ¡ no banco de dados e verifica se a senha bate
        var authentication = manager.authenticate(authenticationToken);
        
        // Se a senha bater, pegamos o usuário
        var usuario = (Usuario) authentication.getPrincipal();
        
        if (!usuario.isEnabled()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body("Sua conta foi bloqueada. Entre em contato com a administração.");
        }
        
        // Geramos a pulseira VIP (Token JWT)
        var tokenJWT = tokenService.gerarToken(usuario);
        
        // Devolvemos o token na tela junto com a role
        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT, usuario.getTipo()));
    }
    
    @org.springframework.beans.factory.annotation.Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity loginComGoogle(@RequestBody DadosGoogleToken dados) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            var jsonFactory = com.google.api.client.json.gson.GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(dados.token());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                Usuario usuario = (Usuario) usuarioRepository.findByEmail(email);

                if (usuario == null) {
                    usuario = new Usuario();
                    usuario.setNome(name);
                    usuario.setEmail(email);
                    usuario.setSenha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    usuario.setTipo("LEITOR");
                    usuario.setStatus(com.bibliotech.api.model.UsuarioStatus.ATIVO);
                    usuario.setPontosGamificacao(0);
                    usuario = usuarioRepository.save(usuario);
                    
                    Notificacao boasVindas = new Notificacao();
                    boasVindas.setUsuario(usuario);
                    boasVindas.setMensagem("OlÃ¡, " + name + "! Bem-vindo(a) Ã  BiblioTech AI! Que bom que conectou com o Google. Explore nosso acervo!");
                    boasVindas.setLida(false);
                    boasVindas.setDataEnvio(java.time.LocalDateTime.now());
                    notificacaoRepository.save(boasVindas);
                }

                if (!usuario.isEnabled()) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body("Sua conta foi bloqueada. Entre em contato com a administração.");
                }

                String tokenJWT = tokenService.gerarToken(usuario);
                return ResponseEntity.ok(new DadosTokenJWT(tokenJWT, usuario.getTipo()));

            } else {
                return ResponseEntity.status(403).body("Token Google invÃ¡lido.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno ao validar token: " + e.getMessage());
        }
    }
    
    // Pequenos Records (DTOs) para estruturar a entrada e saÃ­da do JSON
    public record DadosAutenticacao(String email, String senha) {}
    public record DadosTokenJWT(String token, String role) {}
    public record DadosGoogleToken(String token) {}
}
