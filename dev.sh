#!/bin/sh

set -e

docker compose -f compose.yml -f compose.dev.yml up -d playwright-mcp squid
