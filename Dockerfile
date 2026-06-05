# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Usa un'immagine Maven con Java 21 per compilare il progetto
FROM maven:3.9-eclipse-temurin-21 AS build

# Cartella di lavoro dentro il container
WORKDIR /app

# Copia prima solo il pom.xml e scarica le dipendenze
# (se il codice cambia ma il pom no, Docker usa la cache → build più veloce)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia i file di configurazione dell'analisi statica (richiesti da Checkstyle e SpotBugs)
COPY checkstyle.xml .
COPY spotbugs-exclude.xml .

# Copia il codice sorgente e compila (salta i test per velocizzare la build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
# Immagine finale più leggera: solo il JRE (non tutto Maven)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia solo il JAR prodotto dallo stage di build
COPY --from=build /app/target/*.jar app.jar

# Porta interna del container (come da CONTEXT.md)
EXPOSE 5000

# Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]
