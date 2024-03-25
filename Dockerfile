FROM openjdk
COPY ./build/libs/kurs-0.0.1-SNAPSHOT.jar /app/start.jar
WORKDIR /app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/start.jar"]