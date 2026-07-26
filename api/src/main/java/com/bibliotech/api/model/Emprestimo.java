package com.bibliotech.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "emprestimos")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: Muitos Empréstimos para Um Usuário
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relacionamento: Muitos Empréstimos para Um Livro
    @ManyToOne
    @JoinColumn(name = "id_livro", nullable = false)
    private Livro livro;

    @Column(nullable = false)
    private LocalDate dataRetirada;

    @Column(nullable = false)
    private LocalDate dataDevolucaoPrevista;

    private LocalDate dataDevolucaoReal;

    @Column(nullable = false)
    private String status; // Ex: "ATIVO", "DEVOLVIDO", "ATRASADO"

    private int renovacoesFeitas = 0; // Inicia com 0

    // Campo para armazenar o valor financeiro da multa
    private Double valorMulta;

    // Construtor vazio obrigatório
    public Emprestimo() {
    }

    // Gerar os Getters e Setters para todos os atributos abaixo...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Livro getLivro() { return livro; }
    public void setLivro(Livro livro) { this.livro = livro; }

    public LocalDate getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDate dataRetirada) { this.dataRetirada = dataRetirada; }

    public LocalDate getDataDevolucaoPrevista() { return dataDevolucaoPrevista; }
    public void setDataDevolucaoPrevista(LocalDate dataDevolucaoPrevista) { this.dataDevolucaoPrevista = dataDevolucaoPrevista; }

    public LocalDate getDataDevolucaoReal() { return dataDevolucaoReal; }
    public void setDataDevolucaoReal(LocalDate dataDevolucaoReal) { this.dataDevolucaoReal = dataDevolucaoReal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRenovacoesFeitas() { return renovacoesFeitas; }
    public void setRenovacoesFeitas(int renovacoesFeitas) { this.renovacoesFeitas = renovacoesFeitas; }

    public Double getValorMulta() { return valorMulta; }
    public void setValorMulta(Double valorMulta) { this.valorMulta = valorMulta; }
}