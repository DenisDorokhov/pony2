#!/bin/sh

set -e

docker compose build --no-cache --pull
docker compose up -d --force-recreate