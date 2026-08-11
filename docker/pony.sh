#!/bin/sh

set -e

PONY_UID="${PONY_UID:-1000}"
PONY_GID="${PONY_GID:-1000}"
PONY_TIMEZONE="${PONY_TIMEZONE:-${TZ:-UTC}}"

mkdir -p /home/pony/.pony2
chown "$PONY_UID:$PONY_GID" /home/pony 2>/dev/null || true
chown -R "$PONY_UID:$PONY_GID" /home/pony/.pony2 2>/dev/null || true

exec setpriv --reuid "$PONY_UID" --regid "$PONY_GID" --clear-groups \
  env HOME=/home/pony LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ="$PONY_TIMEZONE" \
  java -Duser.home=/home/pony -jar /home/pony/pony.jar
