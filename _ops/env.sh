#!/bin/bash

ENVIRONMENT=$1
PROJECT_NAME=maiev
ASSEMBLY_NAME=${PROJECT_NAME}-assembly-1.0.jar
DRIVER_IMAGE_NAME=leletan/spark-k8s-driver-${PROJECT_NAME}
EXECUTOR_IMAGE_NAME=leletan/spark-k8s-executor-${PROJECT_NAME}

function check_variable {
    name=$1
    value=$(eval echo \$$name)
    if [ "$value" = "" ]; then
        echo "$name is empty!!!"
        exit 1
    else
        echo "$name=$value"
    fi
}
check_variable ENVIRONMENT

pushd . > /dev/null
cd ..
WORKSPACE=$(pwd -L)
which sbt > /dev/null
if [ $? = 0 ]; then
    SBTCMD=sbt
else
    mkdir ${WORKSPACE}/.ivy2
    SBTCMD="docker run --rm -a stdout -a stderr -v ${WORKSPACE}:/sbt -v ${WORKSPACE}/.ivy2:/root/.ivy2 vungle/sbt"
fi
popd > /dev/null

DEBUG_FLAG=false
FORCE_DRIVER_EXIT=true

if [ "$ENVIRONMENT" != "null" ]; then
  source ${ENVIRONMENT}/env.${ENVIRONMENT}.sh
fi

GITVERSION=$(git rev-parse --short HEAD)
DOCKER_TAG=${ENVIRONMENT}
if [ "$ENVIRONMENT" != "dev" ]; then
    DOCKER_TAG=${ENVIRONMENT}-${GITVERSION}
fi
