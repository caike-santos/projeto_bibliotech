package com.bibliotech.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_livro", nullable = false)
    private Livro livro;

    @Column(nullable = false)
    private LocalDateTime dataSolicitacao;

    @Column(nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ReservaStatus status; // Ex: AGUARDANDO, NOTIFICADO, CANCELADA, CONCLUIDA

    // O @Transient indica que este campo não será persistido no banco de dados, ele é apenas para uso temporário na aplicação.
    @Transient
    private Integer posicaoFila;

    public Reserva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Livro getLivro() { return livro; }
    public void setLivro(Livro livro) { this.livro = livro; }
    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }
    public ReservaStatus getStatus() {
        return status;
    }
    public void setStatus(ReservaStatus status) {
        this.status = status;
    }
    public Integer getPosicaoFila() { return posicaoFila; }
    public void setPosicaoFila(Integer posicaoFila) { this.posicaoFila = posicaoFila; }
}