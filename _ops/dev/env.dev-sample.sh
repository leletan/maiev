#!/bin/bash

# Copy this file to env.test.sh and fill below settings for local integration test.
DEV_NAME=

# Kubenetes master
KUBEMASTER_IP=$(minikube ip)
KUBEMASTER_HOST=${KUBEMASTER_IP}:8443

# AWS credentials
AWS_KEY=
AWS_SECRET=

# S3 configs
S3_BUCKET=
S3_PREFIX=

# Spark settings.
SPARK_NUM_CORES=2
SPARK_EXECUTOR_MEMORY=1G
SPARK_DRIVER_MEMORY=1G

# Zookeeper
ZOOKEEPER_CONNECT=kafka:2181

# Kafka
KAFKA_BROKERS=kafka:9092
KAFKA_TOPICS=topic1
KAFKA_GROUP_ID=maiev_dev
KAFKA_MAX_OFFSET_PER_TRIGGER=20

# TwitterKafkaProducer
ACCESS_TOKEN=
ACCESS_TOKEN_SECRET=
CONSUMER_KEY=
CONSUMER_SECRET=
KEYWORDS_LIST=

# JDBC
JDBC_DRIVER=org.postgresql.Driver
JDBC_URL=jdbc:postgresql://cockroachdb-public:26257/twitter?sslmode=disable
JDBC_USER=leletan
JDBC_PASSWORD=leletan