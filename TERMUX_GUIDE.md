# 📱 Guia de Execução no Celular (Termux)

Siga este passo a passo para rodar a aplicação **BiblioTech AI** integralmente de forma local no seu celular, sem precisar do computador ou internet para hospedar o banco de dados.

## 1. Instalação e Preparação do Ambiente
Abra o aplicativo **Termux** e atualize seus pacotes iniciais:
```bash
pkg update && pkg upgrade -y
```

Agora, instale o Java (JDK 17), o MariaDB, o Git e o Maven:
```bash
pkg install openjdk-17 mariadb git maven -y
```

## 2. Clonando o Projeto
Se você ainda não baixou o projeto no Termux, clone do seu repositório:
```bash
# Permita acesso ao armazenamento do celular, caso precise salvar algo nas pastas
termux-setup-storage

# Clone o repositório
git clone https://github.com/SEU-USUARIO/projeto_bibliotech.git
cd projeto_bibliotech
```

## 3. Configurando o Banco de Dados (MariaDB)
O MariaDB no Termux não roda automaticamente. Você precisa iniciar o servidor e criar a tabela `bibliotech_db` e o usuário.

Abra **duas abas** no Termux (deslizando o canto esquerdo da tela e clicando em "New session").

**Na primeira aba (Servidor do Banco de Dados):**
Inicie o servidor do MariaDB e deixe rodando:
```bash
mariadbd-safe
```

**Na segunda aba (Linha de Comando do Banco):**
Crie o banco de dados e configure o usuário:
```bash
mariadb -u $(whoami)

# Já dentro do console do banco (MariaDB [(none)]>), digite:
CREATE DATABASE IF NOT EXISTS bibliotech_db;
GRANT ALL PRIVILEGES ON bibliotech_db.* TO 'root'@'localhost' IDENTIFIED BY 'root';
FLUSH PRIVILEGES;
EXIT;
```
> O arquivo `application.properties` da API já está configurado para buscar esse usuário `root` e senha `root`.

## 4. Iniciando a API Spring Boot
Na segunda aba do Termux, navegue até a pasta da `api` e use o Maven para rodar o projeto:
```bash
cd ~/projeto_bibliotech/api

# Rode o servidor
mvn clean spring-boot:run
```

Aguarde o Java baixar as dependências (isso pode demorar uns minutos na primeira vez) e você verá o `Tomcat started on port(s): 8080 (http)`.

## 5. Acessando a Interface (Frontend)
Como o backend já está rodando em segundo plano no seu celular (em `localhost:8080`), você pode simplesmente abrir os arquivos HTML da pasta `frontend` pelo seu navegador de celular favorito ou usar um servidor simples.

**Opção recomendada para o Frontend no Termux:**
Abra uma **terceira aba** no Termux, instale e rode um servidor estático:
```bash
pkg install python -y
cd ~/projeto_bibliotech/frontend
python -m http.server 5500
```

Pronto! Agora é só abrir o Chrome/Safari no seu celular, digitar `http://localhost:5500` e usar sua plataforma BiblioTech rodando 100% no seu aparelho!
