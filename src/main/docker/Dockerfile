FROM apache/spark:3.5.0

WORKDIR /app
COPY maven/*.jar ./app.jar

CMD [“java”, “--jar”, “app.jar”]