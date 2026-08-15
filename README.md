<div align="center">
  <img src="./frontend/imagens/logo.png" alt="BiblioTech Logo" width="150" />
  <h1>BiblioTech AI 📚🤖</h1>
  <p><strong>Transformando a gestão de bibliotecas com tecnologia e inteligência artificial</strong></p>
</div>

---

O **BiblioTech AI** é um sistema moderno de gestão de bibliotecas focado em usabilidade, segurança e recomendações avançadas. O projeto integra funcionalidades tradicionais de acervo com inteligência artificial (Gemini) e técnicas avançadas de machine learning para indicar as melhores leituras aos usuários.

## 🚀 Tecnologias Utilizadas

### Backend (API)
- **Java 17+** com **Spring Boot 3**
- **Spring Security** (Autenticação JWT, Controle de Acesso Baseado em Perfis)
- **Spring Data JPA & Hibernate**
- **Google OAuth2** (Login Social com Google)
- Banco de Dados: **PostgreSQL** (Hospedado no Neon)
- **Lumina AI:** Integração Nativa via API com o Google Gemini para recomendações dinâmicas e assistente virtual.

### Frontend
- **HTML5, CSS3, JavaScript Vanilla** 
- Padrão **Single Page Application (SPA)** simplificado sem a necessidade de frameworks pesados.
- UI Responsiva e Moderna com suporte a paleta de cores Dark-Theme.
- Ícones **Phosphor Icons**.

### Segurança
- Transição Segura contra XSS: Tokens **JWT armazenados via Cookies HttpOnly** e SameSite=None.
- Filtro de Segurança customizado em interceptadores Spring para checagem constante.
- Tratamento nativo de CORS liberando acessos apenas para o Frontend oficial (Localhost e GitHub Pages).

---

## 🔑 Principais Regras de Negócio

1. **Gestão de Acesso por Perfis (RBAC):**
   - **`ADMIN`:** Controle total. Pode cadastrar/editar/remover livros, bloquear/desbloquear usuários, confirmar empréstimos e devoluções, e ver todas as métricas do painel.
   - **`BIBLIOTECARIO`:** Gestor focado no acervo físico. Permissões idênticas ao Administrador, com exceção da exclusão em massa e administração da infraestrutura.
   - **`LEITOR`:** Acesso ao catálogo, gamificação, recomendações e emissão de solicitações de empréstimo. Não tem acesso ao Dashboard de Gestão.

2. **Fluxo de Empréstimos e Devoluções:**
   - Leitores apenas *solicitam* (reservam) o empréstimo pelo catálogo virtual.
   - O Livro só é marcado como **"EMPRESTADO"** quando um Bibliotecário confirma a retirada no balcão físico através do Dashboard.
   - Renovações são permitidas **apenas uma única vez** (+14 dias de tolerância).
   - Livros devolvidos entram automaticamente na fila de reserva se houver pessoas aguardando.

3. **Soft Delete (Exclusão Lógica):**
   - Livros e Usuários não somem do banco de dados ao serem "excluídos". O sistema altera a flag de status (ex: status = bloqueado, inativo), mantendo o histórico de auditoria intacto.

4. **Motor de Gamificação:**
   - Leitores conquistam *Badges* (Iniciante, Prata, Ouro, Mestre) com base na contagem de páginas e livros lidos, estimulando o engajamento com a biblioteca.

---

## 🌟 Funcionalidades em Destaque

- **Autenticação Segura e Prática:** Suporte a e-mail e senha tradicionais (Criptografia BCrypt) e um-clique com Login Social do Google.
- **Catálogo Inteligente:** Motor de busca assíncrono para pesquisar por título, autor ou gênero literário sem recarregar a página.
- **Lumina - A Inteligente Assistente:** Chatbot integrado ao catálogo alimentado pelo Google Gemini. A Lumina responde perguntas, sugere livros com base no gosto do leitor e ajuda em dúvidas acadêmicas.
- **Clustering Baseado no Perfil:** Um botão Mágico ("Gerar Recomendações") que usa um algoritmo avançado de cruzamento de preferências para sugerir livros baseado em usuários com gostos similares ("Pessoas que leram X, também leram Y").
- **Dashboard Interativo:** Painel de gestão exclusivo com indicadores gerais, listas de controle, aprovação de empréstimos, gerenciamento de acervo e devoluções simplificadas.

---

## 🛠️ Como Executar o Projeto

### 1. Backend (API)
Navegue até a pasta `api`, configure as variáveis de ambiente necessárias (`application.properties` ou via `.env`) como a senha do banco PostgreSQL e as chaves da API do Google, e rode:
```bash
cd api
./mvnw clean spring-boot:run
```
A API ficará disponível em `http://localhost:8080`.

### 2. Frontend
Não requer build ou transpiladores NodeJS. Basta abrir a pasta `frontend` em um servidor local estático como o **Live Server** (extensão do VS Code).
Ele conectará com o Backend automaticamente utilizando as definições de `API_BASE_URL` descritas no `config.js`.

---

<div align="center">
    <p>Desenvolvido com dedicação para construir bibliotecas do futuro.</p>
</div>
