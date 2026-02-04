# ビルドステージ
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 実行ステージ
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/chatapp-0.0.1-SNAPSHOT.jar app.jar

# 環境変数
ENV PORT=9090
EXPOSE 9090

# 起動コマンド
CMD ["java", "-jar", "app.jar"]
