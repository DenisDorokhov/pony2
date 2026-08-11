#!/bin/sh

set -e

git pull
docker compose build --no-cache --pull
docker compose up -d --force-recreate
