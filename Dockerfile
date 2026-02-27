# ---------- 1. Build frontend ----------
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend .
RUN npm run build


# ---------- 2. Build backend ----------
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

# inject frontend into Spring Boot
COPY --from=frontend-build /frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests


# ---------- 3. Runtime ----------
FROM eclipse-temurin:21-jre

# Install Python + venv support
RUN apt-get update && \
    apt-get install -y python3 python3-venv && \
    apt-get clean

# Create virtual environment
RUN python3 -m venv /opt/venv

# Activate venv and install sympy
RUN /opt/venv/bin/pip install --upgrade pip && \
    /opt/venv/bin/pip install sympy

# Make venv default python
ENV PATH="/opt/venv/bin:$PATH"

WORKDIR /app

COPY --from=backend-build /app/target/engine-0.0.1-SNAPSHOT.jar app.jar
COPY scripts ./scripts

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]