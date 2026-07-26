package com.bibliotech.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bibliotech.api.model.Livro;

@Service
public class IaService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public void enriquecerDadosDoLivro(Livro livro) {
       String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim();

        String titulo = livro.getTitulo() != null ? livro.getTitulo() : "Desconhecido";
        String editora = livro.getEditora() != null ? livro.getEditora() : "Desconhecida";

        // Prompt em linha única, sem quebras \n para não quebrar a requisição do Google
        String prompt = "Atue como um especialista em literatura. Para o livro com ISBN " + livro.getIsbn() 
                + " (Titulo: " + titulo + ", Editora: " + editora + "), responda estritamente em formato JSON com as chaves generoPrincipal, autor e sinopse. Nao use markdown.";

        // Montamos a estrutura JSON para a API do Gemini
        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"") + "\"}]}]}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String respostaJson = restTemplate.postForObject(url, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(respostaJson);

            String respostaIA = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Limpa formatações markdown se a IA enviar
            respostaIA = respostaIA.replace("```json", "").replace("```", "").trim();

            JsonNode dadosExtraidos = mapper.readTree(respostaIA);

            if (dadosExtraidos.has("generoPrincipal")) {
                livro.setGeneroPrincipal(dadosExtraidos.path("generoPrincipal").asText());
            }

            if ((livro.getAutor() == null || livro.getAutor().isEmpty()) && dadosExtraidos.has("autor")) {
                livro.setAutor(dadosExtraidos.path("autor").asText());
            }
            if ((livro.getSinopse() == null || livro.getSinopse().isEmpty()) && dadosExtraidos.has("sinopse")) {
                livro.setSinopse(dadosExtraidos.path("sinopse").asText());
            }

        } catch (Exception e) {
            System.err.println("Aviso: Falha ao enriquecer com IA. " + e.getMessage());
        }
    }

}