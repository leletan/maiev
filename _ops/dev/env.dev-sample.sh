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
