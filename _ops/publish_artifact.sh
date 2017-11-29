#!/bin/bash

ENVIRONMENT=$1

pushd $(dirname $0) > /dev/null
source env.sh $ENVIRONMENT
popd > /dev/null

$SBTCMD clean assembly

docker build -t ${DRIVER_IMAGE_NAME}:${DOCKER_TAG} -f _ops/dockers/driver/Dockerfile .
docker build -t ${EXECUTOR_IMAGE_NAME}:${DOCKER_TAG} -f _ops/dockers/executor/Dockerfile .

if [ "$ENVIRONMENT" != "dev" ]; then
    docker push ${DRIVER_IMAGE_NAME}:${DOCKER_TAG}
    docker push ${EXECUTOR_IMAGE_NAME}:${DOCKER_TAG}
fi


