#!/bin/bash -ex

cp ~/.kube/config .kube/config
docker build -t leletan/spark-k8s-submitter:dev .

rm .kube/config
