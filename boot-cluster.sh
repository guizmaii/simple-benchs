#!/usr/bin/env bash

# https://codewithhugo.com/just-enough-bash-to-be-dangerous/
set -e
set -o pipefail
set -x

docker-compose down
rm -rf /tmp/kafka-streams/
docker system prune --volumes -f

# https://stackoverflow.com/a/39296583
if [[ "${CI}" ]]; then
  docker-compose up > /dev/null
else
  docker-compose up
fi