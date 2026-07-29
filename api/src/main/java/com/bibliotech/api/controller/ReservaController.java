package com.bibliotech.api.controller;

import com.bibliotech.api.model.Reserva;
import com.bibliotech.api.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    // Rota POST usando Query Params (ex: /reservas?usuarioId=1&livroId=5)
    @PostMapping
    public ResponseEntity<Reserva> entrarNaFila(@RequestParam Long usuarioId, @RequestParam Long livroId) {
        Reserva reserva = service.entrarNaFila(usuarioId, livroId);
        return ResponseEntity.ok(reserva);
    }

    // Rota GET para a bibliotecária (ou leitores) verem o tamanho da fila
    @GetMapping("/livro/{livroId}")
    public ResponseEntity<List<Reserva>> verFilaDoLivro(@PathVariable Long livroId) {
        List<Reserva> fila = service.consultarFilaDoLivro(livroId);
        return ResponseEntity.ok(fila);
    }

    // Rota GET para o painel de gestão (todas as reservas)
    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodasReservas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    // Rota GET para o perfil do usuário
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Reserva>> listarReservasDoUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId));
    }
}