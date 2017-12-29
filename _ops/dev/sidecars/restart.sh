#!/bin/bash -ex

set -exuo pipefail

source _ops/dev/env.dev.sh

SIDECAR_DIR=_ops/dev/sidecars

sed "s~\${ACCESS_TOKEN}~${ACCESS_TOKEN}~g;
s~\${ACCESS_TOKEN_SECRET}~${ACCESS_TOKEN_SECRET}~g;
s~\${CONSUMER_KEY}~${CONSUMER_KEY}~g;
s~\${CONSUMER_SECRET}~${CONSUMER_SECRET}~g;
s~\${KEYWORDS_LIST}~${KEYWORDS_LIST}~g;" \
<${SIDECAR_DIR}/tweetkafkaproducer.template >${SIDECAR_DIR}/tweetkafkaproducer.yaml

SIDECAR_POD_CNT=4

kubectl delete --ignore-not-found -f ${SIDECAR_DIR}/kafka.yaml
kubectl delete --ignore-not-found -f ${SIDECAR_DIR}/tweetkafkaproducer.yaml
kubectl delete statefulsets,persistentvolumes,persistentvolumeclaims,services,poddisruptionbudget,job -l app=cockroachdb


while [ $(kubectl get pod -n default -l tier=sidecar | grep 'Terminating' | wc -l) -gt 0 ]
do
    echo "old sidecars are being terminated, waiting..."
    sleep 5
done

echo "old sidecars are terminated"

while [ $(kubectl get persistentvolumes | wc -l) -gt 0 ]
do
    echo "old cockroachdb persistentvolumes are being terminated, waiting..."
    sleep 5
done

kubectl apply -f ${SIDECAR_DIR}/kafka.yaml
kubectl apply -f ${SIDECAR_DIR}/tweetkafkaproducer.yaml
kubectl apply -f ${SIDECAR_DIR}/cockroachdb-statefulset.yaml

while [ $(kubectl get pod -n default -l tier=sidecar | grep '1/1' | grep 'Running' | wc -l) -lt $SIDECAR_POD_CNT ]
do
    echo "sidecar pods are not ready, waiting..."
    sleep 5
done

while [ $(kubectl get persistentvolumes | grep 'Bound' | wc -l) -lt 2 ]
do
    echo "cockroachdb persistentvolumes is not ready, waiting..."
    sleep 5
done

echo "cockroachdb nodes are ready, initializing cluster..."

kubectl apply -f ${SIDECAR_DIR}/cockroachdb-init.yaml

while [ $(kubectl get job cluster-init | wc -l) -lt 2 ]
do
    echo "cockroachdb initialization is not ready, waiting..."
    sleep 5
done

rm ${SIDECAR_DIR}/tweetkafkaproducer.yaml

echo "all sidecars are ready, continue..."

echo "creating tables cockroachdb ... "
kubectl run cockroachdb --attach=true --image=cockroachdb/cockroach --rm --restart=Never -- sql --insecure --host=cockroachdb-public -e \
"CREATE DATABASE IF NOT EXISTS twitter;CREATE TABLE IF NOT EXISTS twitter.user (id INT PRIMARY KEY, follower_cnt INT);
CREATE USER leletan WITH PASSWORD 'leletan'; GRANT ALL ON TABLE twitter.user TO leletan;"
echo "cockroachdb tables created"
