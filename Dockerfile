FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
    JRE_HOME=/usr/lib/jvm/java-21-openjdk-amd64

RUN apt update && apt upgrade -y && apt install openjdk-21-jdk -y && \
    useradd -m pony

COPY backend /home/pony/src/backend
COPY frontend /home/pony/src/frontend
COPY gradle /home/pony/src/gradle
COPY gradlew /home/pony/src
COPY settings.gradle /home/pony/src

RUN cd /home/pony/src && /bin/sh gradlew --no-daemon clean build

RUN cp /home/pony/src/backend/build/libs/`ls /home/pony/src/backend/build/libs | grep -v plain.jar` /home/pony/pony.jar && \
    mkdir /home/pony/.pony2 && chown pony:pony /home/pony/.pony2 && \
    mkdir /home/pony/music && chown pony:pony /home/pony/music

COPY docker/pony.sh /home/pony/pony.sh

ENTRYPOINT ["/home/pony/pony.sh"]
