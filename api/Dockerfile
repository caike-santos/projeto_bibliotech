# Usa uma imagem oficial do Java 17 (versão leve/alpine)
FROM eclipse-temurin:17-jdk-alpine

# Cria uma pasta interna no container chamada /app
WORKDIR /app

# Copia o arquivo .jar gerado pelo Maven para dentro do container
COPY target/*.jar api.jar

# Libera a porta 8080 do container para o mundo exterior
EXPOSE 8080

# O comando que o container vai rodar quando ligar
ENTRYPOINT ["java", "-jar", "api.jar"]