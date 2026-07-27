package com.bibliotech.api.controller;

import com.bibliotech.api.service.IaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assistente")
public class AssistenteController {

    private final IaService iaService;

    public AssistenteController(IaService iaService) {
        this.iaService = iaService;
    }

    // Rota POST para receber a mensagem do usuário e devolver a resposta da Amora
    @PostMapping("/chat")
    public ResponseEntity<String> conversar(@RequestBody DadosMensagem dados) {
        String resposta = iaService.conversarComAssistente(dados.mensagem());
        return ResponseEntity.ok(resposta);
    }
    
    // Record (DTO) simples para mapear o JSON de entrada: { "mensagem": "Como faço um cadastro?" }
    public record DadosMensagem(String mensagem) {}
}