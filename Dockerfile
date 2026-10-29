# =============================================================
# PDV Backend — Dockerfile multi-stage
# =============================================================

# ===== STAGE 1: Build =====
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copia pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S pdv && adduser -S pdv -G pdv

# Instala curl para healthcheck
RUN apk add --no-cache curl

# Copia o jar gerado
COPY --from=builder /build/target/*.jar app.jar

# Ajusta permissões
RUN chown -R pdv:pdv /app
USER pdv

# Porta da aplicação
EXPOSE 8080

# JVM tuning para container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
