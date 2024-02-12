FROM maven:3.8.6-openjdk-8-slim as build
COPY src /root
COPY jmasar-model /root/jmasar-model
COPY pom.xml /root

WORKDIR /root
RUN ls -latr; cd /root/jmasar-model ; mvn -DskipTests clean install
RUN cd /root; mvn clean; mvn install

# FROM openjdk:8-jre as system
# COPY --from=build /root/target/ /target 
# COPY entrypoint.sh /target
# ENTRYPOINT ["/target/entrypoint.sh" ]
