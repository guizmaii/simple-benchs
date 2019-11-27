#!/usr/bin/env bash

set -e
set -o pipefail

script_usage () {
  echo "Usage:"
  echo "  $ $0 <kafka_brokers> <zookeeper_brokers>"
}

if [ -z "$1" ]; then
    script_usage
    exit
fi

if [ -z "$2" ]; then
    script_usage
    exit
fi

curl -s "https://get.sdkman.io" | bash
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
sdk install java 8.0.232.hs-adpt
sdk install sbt
sdk install maven
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"

# -------------------------- #
# BEGIN ---- Schema Registry #
# -------------------------- #
# https://docs.confluent.io/current/installation/installing_cp/rhel-centos.html#get-the-software
# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/add-repositories.html

sudo rpm --import https://packages.confluent.io/rpm/5.3/archive.key

cat <<EOF > confluent.repo
[Confluent.dist]
name=Confluent repository (dist)
baseurl=https://packages.confluent.io/rpm/5.3/7
gpgcheck=1
gpgkey=https://packages.confluent.io/rpm/5.3/archive.key
enabled=1

[Confluent]
name=Confluent repository
baseurl=https://packages.confluent.io/rpm/5.3
gpgcheck=1
gpgkey=https://packages.confluent.io/rpm/5.3/archive.key
enabled=1
EOF

sudo yum-config-manager --add-repo confluent.repo

sudo yum clean all
sudo yum install confluent-platform-2.12 -y

sudo echo ""                                | sudo tee -a /etc/schema-registry/schema-registry.properties # new line https://stackoverflow.com/a/23055893/2431728
sudo echo "kafkastore.bootstrap.servers=$1" | sudo tee -a /etc/schema-registry/schema-registry.properties
sudo echo "kafkastore.connection.url=$2"    | sudo tee -a /etc/schema-registry/schema-registry.properties

/usr/bin/schema-registry-start /etc/schema-registry/schema-registry.properties &

# ------------------------ #
# END ---- Schema Registry #
# ------------------------ #

mv benchs/src/it/resources/benchs.conf benchs/src/it/resources/benchs.old.conf
touch benchs/src/it/resources/benchs.conf
echo "benchs.kafka.bootstrap-servers=\"$1\"" >> benchs/src/it/resources/benchs.conf
echo 'benchs.kafka.schema-registry="http://localhost:8081"' >> benchs/src/it/resources/benchs.conf

./prepare_machine.sh
