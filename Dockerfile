# 构建镜像（容器内编译，无需本机 Maven/源码外其他依赖）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml format.xml ./
COPY src ./src
# 容器内没有 .git，跳过 spotless（ratchet 依赖 git 历史），本机开发仍会检查
RUN --mount=type=cache,target=/root/.m2 mvn -B package -Dspotless.check.skip=true

# 运行镜像：JDK 21 JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 1080
ENTRYPOINT ["java", "-jar", "app.jar"]
