#!/usr/bin/env bash

set -e
set -o pipefail
set -x

git clone https://github.com/guizmaii/simplesource.git
cd simplesource
git checkout missing_public
mvn install --no-transfer-progress
cd ..
git clone https://github.com/guizmaii/simplesource-examples
cd simplesource-examples
git checkout update_simplesource
mvn install --no-transfer-progress
cd ..