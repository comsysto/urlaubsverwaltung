FROM maven:3.9.6-amazoncorretto-21-debian-bookworm AS builder

WORKDIR /app

COPY . /app

RUN mvn clean package

FROM alpine:latest 

RUN apk update && \
    apk add tcpdump

ENV JAVA_HOME /opt/jdk/jdk-21.0.1
ENV PATH $JAVA_HOME/bin:$PATH

ADD https://download.bell-sw.com/java/21.0.1+12/bellsoft-jdk21.0.1+12-linux-x64-musl.tar.gz /opt/jdk/
RUN tar -xzvf /opt/jdk/bellsoft-jdk21.0.1+12-linux-x64-musl.tar.gz -C /opt/jdk/

RUN ["jlink", "--compress=2", \
     "--module-path", "/opt/jdk/jdk-21.0.1/jmods/", \
     "--add-modules", "java.base,java.logging,java.naming,java.desktop,jdk.unsupported", \
     "--no-header-files", "--no-man-pages", \
     "--output", "/springboot-runtime"]

WORKDIR /app

COPY --from=builder /app/target/urlaubsverwaltung-5.1.0-SNAPSHOT.jar /app

EXPOSE 8080

CMD ["java", "-jar", "urlaubsverwaltung-5.1.0-SNAPSHOT.jar"]
