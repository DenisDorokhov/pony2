FROM ubuntu:24.04

ARG PONY_TIMEZONE
ARG PONY_UID=1000
ARG PONY_GID=1000

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    TZ=${PONY_TIMEZONE:-UTC}

RUN apt update && apt install --no-install-recommends tzdata openjdk-21-jdk -y && \
    groupadd -g ${PONY_GID} pony && \
    useradd -m -u ${PONY_UID} -g ${PONY_GID} pony

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
