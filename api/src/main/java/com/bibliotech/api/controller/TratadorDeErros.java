package com.bibliotech.api.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class TratadorDeErros {

    // Erro 404: Quando o sistema tenta buscar algo no banco (livro, usuário) e não encontra
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity tratarErro404() {
        return ResponseEntity.status(404).body(new DadosErroCustomizado(
                "Recurso não encontrado no sistema.", 
                404, 
                LocalDateTime.now()
        ));
    }

    // Erro 400: Quando o usuário envia campos inválidos ou vazios no cadastro
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity tratarErro400(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors();
        var listaDeErros = erros.stream().map(DadosErroValidacao::new).toList();
        
        return ResponseEntity.badRequest().body(listaDeErros);
    }

    // Erro 500: Qualquer exceção genérica não prevista que venha a ocorrer
    @ExceptionHandler(Exception.class)
    public ResponseEntity tratarErro500(Exception ex) {
        return ResponseEntity.status(500).body(new DadosErroCustomizado(
                "Erro interno no servidor: " + ex.getLocalizedMessage(), 
                500, 
                LocalDateTime.now()
        ));
    }

    // Records auxiliares para estruturar o JSON de erro de forma limpa
    private record DadosErroCustomizado(String mensagem, int codigo, LocalDateTime dataHora) {}
    
    private record DadosErroValidacao(String campo, String mensagem) {
        public DadosErroValidacao(FieldError erro) {
            this(erro.getField(), erro.getDefaultMessage());
        }
    }
}