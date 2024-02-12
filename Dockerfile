FROM maven:3.8.6-openjdk-8-slim as build
COPY src /root
COPY pom.xml /root
WORKDIR /root
RUN mvn -DskipTests clean install

FROM openjdk:8-jre as system
COPY --from=build /root/target/ /target 
COPY entrypoint.sh /target
ENTRYPOINT ["/target/entrypoint.sh" ]
