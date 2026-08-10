#!/bin/sh

set -e

mkdir -p /home/pony/.pony2
chown -R pony:pony /home/pony/.pony2

runuser -l pony -c "LC_ALL=C.UTF-8 java -jar /home/pony/pony.jar"
