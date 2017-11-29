#!/bin/bash -ex

ENVIRONMENT=$1

pushd $(dirname $0) > /dev/null
source env.sh $ENVIRONMENT
popd > /dev/null


SIDECAR_CNT=$(ls -1 _ops/${ENVIRONMENT}/sidecar/ | wc -l)

kubectl delete --ignore-not-found -f _ops/${ENVIRONMENT}/sidecar/

while [ $(kubectl get pod -n default -l tier=sidecar | grep 'Terminating' | wc -l) -gt 0 ]
do
    echo "old sidecars are being terminated, waiting..."
    sleep 5
done

echo "old sidecars are terminated"

kubectl apply -f ./_ops/${ENVIRONMENT}/sidecar/
while [ $(kubectl get pod -n default -l tier=sidecar | grep '1/1' | grep 'Running' | wc -l) -lt $SIDECAR_CNT ]
do
    echo "sidecar is not ready, waiting..."
    sleep 5
done

echo "sidecar is ready, continue..."



