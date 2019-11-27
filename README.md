# simple-benchs [![Build Status](https://travis-ci.org/guizmaii/simple-benchs.svg?branch=master)](https://travis-ci.org/guizmaii/simple-benchs)

## How to execute locally

#### 1. Install the required tools

```bash
./prepare_machine.sh
```
   
#### 2. Launch the required infrastructure

```bash
./boot-cluster.sh
```

#### 3. (Optional) Test your Kafka cluster connection

```bash
kafka-topics --list --bootstrap-server kafka-3:29092
```

#### 4. Launch the benchs

We also provide a script for that:

```bash
./exec.sh
```

#### 4. Observe the run

To observe what's happening you should `tail` the benchmark log:

```bash
tail -f result-<timestamp>.log
```

where:
 - `<timestamp>` is the date when you launched the benchmark


## How to execute on AWS

#### 1. Launch the required infrastructure

For that, a CloudFormation template is provided. See `aws_cloudformation_template.yaml` file.

It'll boot an ec2 instance from which you'll be able to launch the benchmarks.    
It'll also boot a MSK cluster that you'll need to run the benchmarks on.

#### 2. SSH on the launched ec2 Instance

```bash
ssh -i <your_key> ec2-user@<instance-ip>
```

#### 3. (Optional) Test your Kafka connection

```bash
./kafka/kafka_2.12-2.2.1/bin/kafka-topics.sh --create --bootstrap-server <bootstrap-servers> --replication-factor 1 --partitions 1 --topic test

./kafka/kafka_2.12-2.2.1/bin/kafka-topics.sh —-list --bootstrap-server <bootstrap-servers>
```

where:
 - `<bootstrap-servers>` is your MSK (plaintext) bootstrap servers info

#### 4. Git clone this repo on the ec2 instance

```bash
git clone https://github.com/guizmaii/simple-benchs
cd simple-benchs
```

#### 5. Install the required tools

We provide a script for that:

```bash
./aws_prepare_machine.sh <kafka_brokers> <zookeeper_brokers>
```

where:
 - `<kafka_brokers>` is your MSK (plaintext) bootstrap servers info
 - `<zookeeper_brokers>` is you MSK Zookeeper connect info

#### 6. Launch the benchs

We also provide a script for that:

```bash
./exec.sh
```

#### 7. Observe the run

To observe what's happening you should `tail` the benchmark log:

```bash
tail -f result-<timestamp>.log
```

where:
 - `<timestamp>` is the date when you launched the benchmark

## FAQ

#### `./exec.sh` fails with the following message: ./exec.sh: line 8: sbt: command not found

Sdkman is not in your PATH, to fix that:

```bash
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
```