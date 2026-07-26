package com.bibliotech.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // Avisa ao Spring que esta classe vai escutar todos os erros da API
public class TratadorDeErros {

    @ExceptionHandler(RuntimeException.class) // Captura qualquer RuntimeException que nós lançarmos
    public ResponseEntity<String> tratarRegraDeNegocio(RuntimeException ex) {
        // Muda o status para 400 (Bad Request) e devolve apenas a nossa mensagem de texto
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}