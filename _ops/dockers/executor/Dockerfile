FROM kubespark/spark-base:v2.2.0-kubernetes-0.3.0

ENV ASSEMBLY_NAME=maiev-assembly-1.0.jar
ENV JAR_PATH=target/scala-2.11/$ASSEMBLY_NAME

ADD $JAR_PATH /opt/spark/jars/$ASSEMBLY_NAME

CMD SPARK_CLASSPATH="${SPARK_HOME}/jars/*" && \
    if ! [ -z ${SPARK_MOUNTED_CLASSPATH}+x} ]; then SPARK_CLASSPATH="$SPARK_MOUNTED_CLASSPATH:$SPARK_CLASSPATH"; fi && \
    if ! [ -z ${SPARK_EXECUTOR_EXTRA_CLASSPATH+x} ]; then SPARK_CLASSPATH="$SPARK_EXECUTOR_EXTRA_CLASSPATH:$SPARK_CLASSPATH"; fi && \
    if ! [ -z ${SPARK_EXTRA_CLASSPATH+x} ]; then SPARK_CLASSPATH="$SPARK_EXTRA_CLASSPATH:$SPARK_CLASSPATH"; fi && \
    if ! [ -z ${SPARK_MOUNTED_FILES_DIR} ]; then cp -R "$SPARK_MOUNTED_FILES_DIR/." .; fi && \
    exec /sbin/tini -- ${JAVA_HOME}/bin/java -Dspark.executor.port=$SPARK_EXECUTOR_PORT -Xms$SPARK_EXECUTOR_MEMORY -Xmx$SPARK_EXECUTOR_MEMORY -cp $SPARK_CLASSPATH org.apache.spark.executor.CoarseGrainedExecutorBackend --driver-url $SPARK_DRIVER_URL --executor-id $SPARK_EXECUTOR_ID --cores $SPARK_EXECUTOR_CORES --app-id $SPARK_APPLICATION_ID --hostname $SPARK_EXECUTOR_POD_IP
