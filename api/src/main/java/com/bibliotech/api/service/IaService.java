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
import com.bibliotech.api.model.Tag;
import com.bibliotech.api.repository.TagRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IaService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final TagRepository tagRepository;

    // Injeção de dependência do TagRepository para salvar e buscar tags no banco
    public IaService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public void enriquecerDadosDoLivro(Livro livro) {
       String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim();

        String titulo = livro.getTitulo() != null ? livro.getTitulo() : "Desconhecido";
        String editora = livro.getEditora() != null ? livro.getEditora() : "Desconhecida";

        // Prompt atualizado para exigir o array de tagsSecundarias
        String prompt = "Atue como um especialista em literatura. Para o livro com ISBN " + livro.getIsbn() 
                + " (Titulo: " + titulo + ", Editora: " + editora + "), responda estritamente em formato JSON com as chaves generoPrincipal, autor, sinopse, ano (apenas o numero), capaUrl (um link de imagem valido) e tagsSecundarias (um array de strings com 3 palavras-chave curtas sobre a tematica do livro). Nao use markdown.";

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
            
            if (livro.getAno() == null && dadosExtraidos.hasNonNull("ano")) {
                livro.setAno(dadosExtraidos.path("ano").asInt());
            }
            
            if ((livro.getCapaUrl() == null || livro.getCapaUrl().isEmpty()) && dadosExtraidos.hasNonNull("capaUrl")) {
                livro.setCapaUrl(dadosExtraidos.path("capaUrl").asText());
            }

            // --- NOVA EXTRAÇÃO: TAGS SECUNDÁRIAS ---
            if (dadosExtraidos.has("tagsSecundarias") && dadosExtraidos.path("tagsSecundarias").isArray()) {
                List<Tag> tagsDoLivro = new ArrayList<>();
                
                for (JsonNode tagNode : dadosExtraidos.path("tagsSecundarias")) {
                    String nomeTag = tagNode.asText().trim();
                    
                    if (!nomeTag.isEmpty()) {
                        // Busca no banco ignorando maiúsculas/minúsculas
                        Optional<Tag> tagExistente = tagRepository.findByNomeIgnoreCase(nomeTag);
                        
                        if (tagExistente.isPresent()) {
                            // Se a tag já existe no sistema, apenas adiciona ao livro
                            tagsDoLivro.add(tagExistente.get());
                        } else {
                            // Se for uma tag nova, cria, salva no banco de dados e adiciona ao livro
                            Tag novaTag = new Tag(nomeTag);
                            novaTag = tagRepository.save(novaTag);
                            tagsDoLivro.add(novaTag);
                        }
                    }
                }
                
                // Vincula a lista de tags processadas ao livro
                livro.setTagsSecundarias(tagsDoLivro);
            }

        } catch (Exception e) {
            System.err.println("Aviso: Falha ao enriquecer com IA. " + e.getMessage());
        }
    }

    public String gerarRecomendacoesParaUsuario(java.util.List<String> livrosLidos, String catalogoDisponivel) {
        if (livrosLidos == null || livrosLidos.isEmpty()) {
            return "Parece que você ainda não pegou nenhum livro emprestado. Que tal começar explorando nosso catálogo?";
        }

        if (catalogoDisponivel == null || catalogoDisponivel.isEmpty()) {
            return "Nosso acervo está passando por uma atualização no momento. Volte em breve para novas recomendações!";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim();

        // Monta o texto dos livros lidos
        String titulos = String.join(", ", livrosLidos);

        // Prompt blindado: A IA é obrigada a escolher apenas os livros que enviamos no catálogo
        String prompt = "Atue como um bibliotecário especialista. Um leitor leu os seguintes livros: " + titulos + 
                        ". Recomende 3 livros para ele ler a seguir, escolhendo EXCLUSIVAMENTE desta lista do nosso acervo atual: [" + catalogoDisponivel + "]. " +
                        "Faça um texto amigável em português, justificando brevemente a indicação. É estritamente proibido recomendar qualquer livro que não esteja na lista do acervo fornecida. Não use markdown.";

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"") + "\"}]}]}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String respostaJson = restTemplate.postForObject(url, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(respostaJson);
            
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
        } catch (Exception e) {
            return "Ops, nossa bibliotecária virtual está indisponível no momento. Continue explorando o catálogo!";
        }
    }

    public String conversarComAssistente(String mensagemUsuario) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim();

        // O "Cérebro" do Bot: Configurando a personalidade da Lumina
        String promptSistema = "Você é a Lumina, uma coruja cibernética virtual e assistente inteligente da biblioteca BiblioTech. "
                + "Responda de forma amigável, culta, direta e prestativa. Ocasionalmente, você pode fazer referências muito sutis e divertidas à sua natureza cibernética (como processar dados, escanear prateleiras ou ajustar seus sensores visuais). "
                + "Regras da biblioteca: limite de 3 livros por leitor, prazo de devolução de 14 dias, bloqueio por atraso e 1 renovação permitida (desde que sem fila de espera). "
                + "Se a pessoa perguntar onde encontrar algo, diga que pode navegar pelo menu superior do portal. "
                + "A pergunta do leitor é: " + mensagemUsuario;

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + promptSistema.replace("\"", "\\\"") + "\"}]}]}";

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        try {
            String respostaJson = restTemplate.postForObject(url, entity, String.class);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(respostaJson);
            
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
        } catch (Exception e) {
            return "Au au! Desculpe, estou com problemas de conexão no momento. Tente me chamar de novo em alguns minutos!";
        }
    }
}