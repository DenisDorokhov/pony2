#!/bin/sh

docker compose -f compose.yml -f compose.dev.yml up -d playwright-mcp squid

echo
echo "Press Enter to close"
read _
