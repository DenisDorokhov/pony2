FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    TZ=UTC

RUN apt update && apt install --no-install-recommends tzdata openjdk-21-jdk util-linux -y

COPY backend /home/pony/src/backend
COPY frontend /home/pony/src/frontend
COPY gradle /home/pony/src/gradle
COPY gradlew /home/pony/src
COPY settings.gradle /home/pony/src

RUN cd /home/pony/src && /bin/sh gradlew --no-daemon clean build

RUN cp /home/pony/src/backend/build/libs/`ls /home/pony/src/backend/build/libs | grep -v plain.jar` /home/pony/pony.jar && \
    mkdir -p /home/pony/.pony2 /home/pony/music

COPY docker/pony.sh /home/pony/pony.sh

ENTRYPOINT ["/home/pony/pony.sh"]
