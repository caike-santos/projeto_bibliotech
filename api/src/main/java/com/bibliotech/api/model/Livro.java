package com.bibliotech.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "livros")
public class Livro {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String isbn;

    private String titulo;

    private String autor;

    private String editora;

    private Integer ano;

    private String capaUrl;

    @Column(columnDefinition = "TEXT")

    private String sinopse;

    private String generoPrincipal;

    private Integer quantidadeTotal;

    private Integer quantidadeDisponivel;

    public Livro(){
    }

    public Integer getAno() {
        return ano;
    }
    public void setAno(Integer ano) {
        this.ano = ano;
    }
    public String getAutor() {
        return autor;
    }
    public void setAutor(String autor) {
        this.autor = autor;
    }
    public String getCapaUrl() {
        return capaUrl;
    }
    public void setCapaUrl(String capaUrl) {
        this.capaUrl = capaUrl;
    }
    public String getEditora() {
        return editora;
    }
    public void setEditora(String editora) {
        this.editora = editora;
    }
    public String getGeneroPrincipal() {
        return generoPrincipal;
    }
    public void setGeneroPrincipal(String generoPrincipal) {
        this.generoPrincipal = generoPrincipal;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    public Integer getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }
    public void setQuantidadeDisponivel(Integer quantidadeDisponivel) {
        this.quantidadeDisponivel = quantidadeDisponivel;
    }
    public Integer getQuantidadeTotal() {
        return quantidadeTotal;
    }
    public void setQuantidadeTotal(Integer quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }
    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }
    public String getSinopse() {
        return sinopse;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getTitulo() {
        return titulo;
    }
}
