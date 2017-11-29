#!/bin/bash -ex

ENVIRONMENT=$1
JOB=$2
VERSION=$3
CPATH=`PWD`
pushd $(dirname $0) > /dev/null
source env.sh ${ENVIRONMENT}

case $JOB in
    KafkaSourceTest)
        JOB_NAME=Maiev.KafkaSourceTest
        MAIN_CLASS=org.leletan.maiev.job.KafkaSourceTest
        SPARK_DRIVER_POD_NAME=$(echo "Spark-Driver-${PROJECT_NAME}-${ENVIRONMENT}-${JOB}" | awk '{print tolower($0)}')
        ;;
    *)
    	echo "Bad job: $JOB"
        exit 1
        ;;
esac

case $ENVIRONMENT in
    prod)
        JOB_NAME=$(echo "PROD-${JOB_NAME}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=prod
        ;;
    qa)
        JOB_NAME=$(echo "QA-${JOB_NAME}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=qa
        ;;
    dev)
        JOB_NAME=$(echo "DEV-${JOB_NAME}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=default
        ;;
    *)
    	echo "Bad environment: $ENVIRONMENT"
        exit 1
        ;;
esac

# Build submitter
cd ${ENVIRONMENT}/submitter
bash build.sh
cd $CPATH

# Clean up
docker stop spark-submitter || true && docker rm spark-submitter || true
kubectl delete --ignore-not-found -n ${KUBERNETES_NAMESPACE} pod ${SPARK_DRIVER_POD_NAME}
while [ $(kubectl get pod -n ${KUBERNETES_NAMESPACE} ${SPARK_DRIVER_POD_NAME} | wc -l) -gt 1 ]
do
    sleep 5
done

# Submit
docker run --rm --name spark-submitter \
        leletan/spark-k8s-submitter:${ENVIRONMENT} \
        bin/spark-submit \
        --deploy-mode cluster \
        --class ${MAIN_CLASS} \
        --master k8s://https://${KUBEMASTER_HOST} \
        --kubernetes-namespace ${KUBERNETES_NAMESPACE} \
        --conf spark.kubernetes.driver.pod.name=${SPARK_DRIVER_POD_NAME} \
        --conf spark.executor.instances=2 \
        --conf spark.app.name=${JOB_NAME} \
        --conf spark.cores.max=${SPARK_NUM_CORES} \
        --conf spark.executor.memory=${SPARK_EXECUTOR_MEMORY} \
        --conf spark.driver.memory=${SPARK_DRIVER_MEMORY} \
        --conf spark.kubernetes.driver.docker.image=${DRIVER_IMAGE_NAME}:${DOCKER_TAG} \
        --conf spark.kubernetes.executor.docker.image=${EXECUTOR_IMAGE_NAME}:${DOCKER_TAG}  \
        --conf spark.kubernetes.initcontainer.docker.image=kubespark/spark-init:v2.2.0-kubernetes-0.3.0 \
        local:///opt/spark/jars/${ASSEMBLY_NAME}

popd > /dev/null

