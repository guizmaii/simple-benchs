#!/usr/bin/env bash

set -e
set -o pipefail

# Tested on Macos only. I don't know if the `date` format I use will work correctly on Linux.

sbt gatling-it:test > "result-$(date +"%Y-%m-%dT%H:%M:%S%z").log"