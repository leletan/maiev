#!/bin/bash -ex

ENVIRONMENT=$2

pushd $(dirname $0) > /dev/null
source env.sh $ENVIRONMENT
popd > /dev/null

SIDECAR_DIR=_ops/${ENVIRONMENT}/sidecar

sed "s~\${ACCESS_TOKEN}~${ACCESS_TOKEN}~g;
s~\${ACCESS_TOKEN_SECRET}~${ACCESS_TOKEN_SECRET}~g;
s~\${CONSUMER_KEY}~${CONSUMER_KEY}~g;
s~\${CONSUMER_SECRET}~${CONSUMER_SECRET}~g;
s~\${KEYWORDS_LIST}~${KEYWORDS_LIST}~g;" \
<${SIDECAR_DIR}/tweetkafkaproducer.template >${SIDECAR_DIR}/tweetkafkaproducer.yaml

SIDECAR_POD_CNT=$(ls -1 _ops/${ENVIRONMENT}/sidecar/ | grep 'kind: Pod' | wc -l)

kubectl delete --ignore-not-found -f ${SIDECAR_DIR}/

while [ $(kubectl get pod -n default -l tier=sidecar | grep 'Terminating' | wc -l) -gt 0 ]
do
    echo "old sidecars are being terminated, waiting..."
    sleep 5
done

echo "old sidecars are terminated"

kubectl apply -f ./${SIDECAR_DIR}/
while [ $(kubectl get pod -n default -l tier=sidecar | grep '1/1' | grep 'Running' | wc -l) -lt $SIDECAR_POD_CNT ]
do
    echo "sidecar is not ready, waiting..."
    sleep 5
done

echo "sidecar is ready, continue..."

rm ${SIDECAR_DIR}/tweetkafkaproducer.yaml
