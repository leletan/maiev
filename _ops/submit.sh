#!/bin/bash -ex

ENVIRONMENT=$1
JOB=$2
CPATH=`PWD`
pushd $(dirname $0) > /dev/null
source env.sh ${ENVIRONMENT}


echo "job: $2"

case $JOB in
    twitter_follower_jdbc_logging)
        JOB_NAME=Maiev.TwitterFollwerJDBCLogging
        MAIN_CLASS=org.leletan.maiev.job.TwitterFollowerJDBCLogging
        SPARK_DRIVER_POD_NAME=$(echo "Spark-Driver-${PROJECT_NAME}-${ENVIRONMENT}-Twitter-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        ;;
    reliable_s3_parquet_logging)
        JOB_NAME=Maiev.ReiliableS3ParquetLogging
        MAIN_CLASS=org.leletan.maiev.job.ReliableS3ParquetLogging
        SPARK_DRIVER_POD_NAME=$(echo "Spark-Driver-${PROJECT_NAME}-${ENVIRONMENT}-S3Parquet-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        ;;
    redshift_to_redis)
        JOB_NAME=Maiev.RedshiftToRedis
        MAIN_CLASS=org.leletan.maiev.job.RedshiftToRedis
        SPARK_DRIVER_POD_NAME=$(echo "Spark-Driver-${PROJECT_NAME}-${ENVIRONMENT}-Rdshft2Rdis-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        ;;
    *)
    	echo "Bad job: $JOB"
        exit 1
        ;;
esac

KAFKA_GROUP_ID=${KAFKA_GROUP_ID_PREFIX}_${JOB}

case $ENVIRONMENT in
    prod)
        JOB_NAME=$(echo "PROD-${JOB_NAME}-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=prod
        ;;
    qa)
        JOB_NAME=$(echo "QA-${JOB_NAME}-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=qa
        ;;
    dev)
        JOB_NAME=$(echo "DEV-${JOB_NAME}-${JOB_SUFFIX}" | awk '{print tolower($0)}')
        KUBERNETES_NAMESPACE=prod
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
        --conf spark.executor.instances=${POD_INSTANCE_COUNT} \
        --conf spark.app.name=${JOB_NAME} \
        --conf spark.cores.max=${SPARK_NUM_CORES} \
        --conf spark.executor.memory=${SPARK_EXECUTOR_MEMORY} \
        --conf spark.driver.memory=${SPARK_DRIVER_MEMORY} \
        --conf spark.kubernetes.executor.limit.cores=${SPARK_NUM_CORES} \
        --conf spark.sql.shuffle.partitions=${SPARK_SQL_SHUFFLE_PARTITIONS} \
        --conf spark.default.parallelism=${SPARK_DEFAULT_PARALLELISM} \
        --conf spark.kubernetes.driver.docker.image=${DRIVER_IMAGE_NAME}:${DOCKER_TAG} \
        --conf spark.kubernetes.executor.docker.image=${EXECUTOR_IMAGE_NAME}:${DOCKER_TAG}  \
        --conf spark.hadoop.fs.s3a.access.key=${AWS_KEY} \
        --conf spark.hadoop.fs.s3a.secret.key=${AWS_SECRET} \
        --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
        --conf spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version=2 \
        --conf spark.hadoop.parquet.enable.summary-metadata=false \
        --conf spark.speculation=false \
        --conf spark.task.maxFailures=10 \
        --conf spark.s3.bucket=${S3_BUCKET} \
        --conf spark.s3.prefix=${S3_PREFIX} \
        --conf spark.jdbc.driver=${JDBC_DRIVER} \
        --conf spark.jdbc.url=${JDBC_URL} \
        --conf spark.jdbc.user=${JDBC_USER} \
        --conf spark.jdbc.password=${JDBC_PASSWORD} \
        --conf spark.streaming.zookeeper.connect=${ZOOKEEPER_CONNECT} \
        --conf spark.streaming.kafka.broker.list=${KAFKA_BROKERS} \
        --conf spark.streaming.kafka.topics=${KAFKA_TOPICS} \
        --conf spark.streaming.kafka.group.id=${KAFKA_GROUP_ID} \
        --conf spark.streaming.kafka.max.offsets.per.trigger=${KAFKA_MAX_OFFSET_PER_TRIGGER} \
        --conf spark.job.log.level=${JOB_LOG_LEVEL} \
        --conf spark.redshift.db.name=${REDSHIFT_DB_NAME} \
        --conf spark.redshift.user.id=${REDSHIFT_USER_ID} \
        --conf spark.redshift.password=${REDSHIFT_PASSWOR} \
        --conf spark.redshift.url=${REDSHIFT_URL} \
        --conf spark.redshift.temp.s3.dir=${REDSHIFT_TEMP_S3_DIR} \
        --conf spark.redshift2redis.start="${R2R_START_DATE}" \
        --conf spark.redshift2redis.end="${R2R_END_DATE}" \
        --conf "spark.driver.extraJavaOptions=-Dredis.max.idel.per.pool=${REDIS_MAX_IDLE} -Dredis.max.conn.per.pool=${REDIS_MAX_CONN} -Dredis.timeout.in.millis=${REDIS_TIMEOUT} -Dredis.max.rate.per.pool=${REDIS_MAX_RATE} -Dredshift2redis.bf.sha=${R2R_BF_SHA} -Dredshift2redis.retry.cnt=${R2R_RETRY_CNT} -Dstats.host=${STATS_CLIENT} -Dstats.prefix=${STATS_PREFIX} -Dredis.host=${REDIS_HOST} -Dredis.port=${REDIS_PORT} -Dredis.auth=${REDIS_AUTH} -Dredis.db=${REDIS_DB}" \
        --conf "spark.executor.extraJavaOptions=-Dredis.max.idel.per.pool=${REDIS_MAX_IDLE} -Dredis.max.conn.per.pool=${REDIS_MAX_CONN} -Dredis.timeout.in.millis=${REDIS_TIMEOUT} -Dredis.max.rate.per.pool=${REDIS_MAX_RATE} -Dredshift2redis.bf.sha=${R2R_BF_SHA} -Dredshift2redis.retry.cnt=${R2R_RETRY_CNT} -Dstats.host=${STATS_CLIENT} -Dstats.prefix=${STATS_PREFIX} -Dredis.host=${REDIS_HOST} -Dredis.port=${REDIS_PORT} -Dredis.auth=${REDIS_AUTH} -Dredis.db=${REDIS_DB}" \
        local:///opt/spark/jars/${ASSEMBLY_NAME}

popd > /dev/null

