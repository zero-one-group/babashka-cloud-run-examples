FROM clojure:openjdk-8-lein-2.9.1

COPY . /app
WORKDIR /app

RUN cd /app && lein uberjar

EXPOSE 8080

CMD ["java", "-jar", "/app/target/uberjar/app.jar"]
