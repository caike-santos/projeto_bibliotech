package com.bibliotech.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bibliotech.api.model.Livro;

@Service
public class LivroApiService {

    public Livro buscarDadosDoLivroPorIsbn(String isbn) {
        // 1. Usando a Brasil API (Totalmente aberta, gratuita e em português)
        String url = "https://brasilapi.com.br/api/isbn/v1/" + isbn;

        RestTemplate restTemplate = new RestTemplate();
        
        try {
            // 2. Faz a requisição GET
            String respostaJson = restTemplate.getForObject(url, String.class);

            // 3. Lê o JSON retornado
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(respostaJson);

            // 4. Monta o objeto Livro mapeando os campos da Brasil API
            Livro livro = new Livro();
            livro.setIsbn(isbn);
            livro.setTitulo(root.path("title").asText(null));
            
            // A Brasil API devolve os autores em uma lista (array)
            if (root.path("authors").isArray() && root.path("authors").size() > 0) {
                livro.setAutor(root.path("authors").get(0).asText());
            }

            livro.setEditora(root.path("publisher").asText(null));
            livro.setSinopse(root.path("synopsis").asText(null));

            return livro;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar o livro. Verifique se o ISBN está correto ou se há conexão: " + e.getMessage());
        }
    }
}