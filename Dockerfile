FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Instalar Java y Maven
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiamos TODO el proyecto
COPY . /app

# Asegurar que el wrapper es ejecutable y compilar
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw && \
    ./mvnw -B clean package -DskipTests

EXPOSE 8080

# Ejecutar el jar generado en target
CMD ["sh", "-c", "java -jar target/*.jar"]
