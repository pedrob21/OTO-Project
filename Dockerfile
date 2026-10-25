# Etapa 1: gera o jar com o Gradle Wrapper do projeto
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
# Os testes precisam do banco no ar, então ficam fora do build da imagem.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Etapa 2: imagem final só com o JRE e o jar
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system oto && useradd --system --gid oto oto
COPY --from=build /workspace/build/libs/*.jar app.jar
USER oto
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
