package com.bibliotech.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteController {

    @GetMapping("/ola")
    public String dizerOla() {
        return "Olá, BiblioTech AI! O servidor Spring Boot está funcionando!";
    }
}
