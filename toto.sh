#!/usr/bin/env bash

script_usage () {
  echo "Usage:"
  echo "  $ $0 kafka_brokers zookeeper_brokers"
}

if [ -z "$1" ]; then
    script_usage
    exit
fi

if [ -z "$2" ]; then
    script_usage
    exit
fi

echo ""   | tee -a toto.txt
echo "$1" | tee -a toto.txt
echo "$2" | tee -a toto.txt