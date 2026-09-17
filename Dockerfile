FROM eclipse-temurin:21-jre
ARG JAR
WORKDIR /app
COPY ${JAR} app.jar
ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
