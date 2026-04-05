# Этап сборки
FROM maven:3.8-eclipse-temurin-8 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Сборка проекта
RUN mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
# Копируем собранный jar
COPY --from=builder /app/target/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]