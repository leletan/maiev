#!/bin/bash -ex

ENVIRONMENT=$1
JOB=$2
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
    echo "===== old spark driver still running, waiting ... ====="
    sleep 5
done

echo "===== oldspark driver deleted, submitting ... ====="

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
        --conf spark.hadoop.fs.s3a.access.key=${AWS_KEY} \
        --conf spark.hadoop.fs.s3a.secret.key=${AWS_SECRET} \
        --conf spark.s3.bucket=${S3_BUCKET} \
        --conf spark.s3.prefix=${S3_PREFIX} \
        --conf spark.streaming.zookeeper.connect=${ZOOKEEPER_CONNECT} \
        --conf spark.streaming.kafka.broker.list=${KAFKA_BROKERS} \
        --conf spark.streaming.kafka.topics=${KAFKA_TOPICS} \
        --conf spark.streaming.kafka.group.id=${KAFKA_GROUP_ID} \
        local:///opt/spark/jars/${ASSEMBLY_NAME}

#while [ $(kubectl get pod -n ${KUBERNETES_NAMESPACE} ${SPARK_DRIVER_POD_NAME} | grep 1/1 | wc -l) -lt 1 ]
#do
#    echo "===== new spark driver not up, waiting ... ====="
#    sleep 5
#done
#
#echo "To view spark job dashboard, run kubectl port-forward ${SPARK_DRIVER_POD_NAME} 4040:4040"
popd > /dev/null

