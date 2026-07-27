package com.bibliotech.api.model;

import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.*;

@Entity // Avisa ao Spring que esta classe vai virar uma tabela no banco
@Table(name = "usuarios") // Define o nome exato da tabela
public class Usuario implements org.springframework.security.core.userdetails.UserDetails{

    @Id // Diz que este campo é a Chave Primária (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O banco vai gerar o ID automaticamente (Auto Increment)
    private Long id;

    @Column(nullable = false, length = 100) // Não pode ser nulo, máximo 100 caracteres
    private String nome;

    @Column(nullable = false, unique = true, length = 100) // E-mail único para cada usuário
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String tipo; // ADMIN, BIBLIOTECARIO ou LEITOR

    @Column(nullable = false)
    private String status; // ATIVO ou BLOQUEADO

    private int pontosGamificacao = 0; // Inicia com 0 pontos

    private boolean enabled = true;

    // CONSTRUTOR VAZIO (Obrigatório para o Spring funcionar)
    public Usuario() {
    }

    // GETTERS E SETTERS (Obrigatórios para acessar os dados privados)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPontosGamificacao() { return pontosGamificacao; }
    public void setPontosGamificacao(int pontosGamificacao) { this.pontosGamificacao = pontosGamificacao; }

    public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    
}

    @Override
    public boolean isEnabled() {
       return this.enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(); // Devolve uma lista vazia (sem restrição de perfis por enquanto)
    }

    @Override
    public String getPassword() {
        return senha; // Aponta para a variável que guarda a senha no seu banco
    }

    @Override
    public String getUsername() {
        return email;
    }

    
}