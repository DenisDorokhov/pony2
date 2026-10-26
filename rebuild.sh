#!/bin/sh

set -e

git pull
if [ "$1" = "--quiet" ]; then
    docker compose build --no-cache --pull --quiet
else
    docker compose build --no-cache --pull
fi
docker compose up -d --force-recreate
