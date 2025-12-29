FROM groovy:3.0.9-jdk11
WORKDIR /app
COPY . /app
RUN ./gradlew assemble
CMD ["java", "-jar", "build/libs/todo-list-0.1.jar"]