#!/bin/sh

set -e

git pull
if [ "$1" = "--quiet" ]; then
    docker compose build --no-cache --pull --quiet
    docker compose --progress quiet up -d --force-recreate
else
    docker compose build --no-cache --pull
    docker compose up -d --force-recreate
fi
