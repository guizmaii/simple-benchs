#!/usr/bin/env bash

docker-compose down
rm -rf /tmp/kafka-streams/
docker system prune --volumes -f
docker-compose up