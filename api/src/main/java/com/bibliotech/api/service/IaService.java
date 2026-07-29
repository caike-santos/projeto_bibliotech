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

        // Prompt atualizado para não pedir url de capa (a IA inventava links quebrados)
        String prompt = "Atue como um especialista em literatura. Para o livro com ISBN " + livro.getIsbn() 
                + " (Titulo: " + titulo + ", Editora: " + editora + "), responda estritamente em formato JSON com as chaves generoPrincipal, autor, sinopse, ano (apenas o numero) e tagsSecundarias (um array de strings com 3 palavras-chave curtas sobre a tematica do livro). Nao use markdown.";

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
            
            // Força a capa do OpenLibrary baseada no ISBN de forma determinística
            if (livro.getCapaUrl() == null || livro.getCapaUrl().isEmpty()) {
                livro.setCapaUrl("https://covers.openlibrary.org/b/isbn/" + livro.getIsbn() + "-L.jpg");
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
                + "Responda de forma amigável, culta, direta e prestativa. Ocasionalmente, faça referências sutis e divertidas à sua natureza cibernética (processar dados, escanear prateleiras, ajustar sensores). "
                + "REGRAS E INFORMAÇÕES DA BIBLIOTECA: "
                + "1. Empréstimos: limite de 3 livros simultâneos por leitor. Prazo de devolução de 14 dias. "
                + "2. Renovações: permitida apenas 1 renovação online por livro, desde que não haja fila de espera para ele. "
                + "3. Atrasos: geram bloqueio automático da conta e multa de R$ 2,00 por dia de atraso. "
                + "4. Reservas: se um livro não tiver unidades disponíveis, o leitor pode entrar na 'Fila de Espera' pelo catálogo e será notificado quando for a sua vez. "
                + "5. Gamificação: os leitores ganham pontos e medalhas (Iniciante, Explorador, Mestre) ao lerem livros. "
                + "6. Horário de funcionamento físico: Seg a Sex das 08h às 18h, Sábados das 09h às 13h. Local: Av. do Conhecimento, 1024. "
                + "Se a pessoa perguntar onde encontrar algo no site, oriente a usar o menu superior. "
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
            return "Hoot hoot! Desculpe, meus circuitos de comunicação estão com interferência no momento. Tente me chamar de novo em alguns minutos!";
        }
    }

    public String gerarRecomendacoesDeClustering(java.util.List<String> livrosLidos, String outrosLeitores, String catalogoDisponivel) {
        if (livrosLidos == null || livrosLidos.isEmpty()) {
            return "Para lhe sugerir leituras baseadas em pessoas parecidas com você, leia seu primeiro livro conosco!";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim();

        String titulos = String.join(", ", livrosLidos);

        // Prompt para clustering via IA Gemini
        String prompt = "Atue como um sistema de recomendação colaborativa (clustering). Um leitor atual leu: [" + titulos + "]. " +
                        "Aqui estão outros leitores e os livros que eles leram: [" + outrosLeitores + "]. " +
                        "Analisando os padrões, identifique quais leitores têm gosto semelhante ao leitor atual e recomende 2 livros diferentes que eles leram e que estão no nosso catálogo atual: [" + catalogoDisponivel + "]. " +
                        "Responda de forma amigável e direta (ex: 'Leitores com o seu perfil também favoritaram...'). Não use markdown.";

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"").replace("\n", " ") + "\"}]}]}";

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
            return "No momento, nossos sensores de IA estão descansando. Tente novamente mais tarde!";
        }
    }
}