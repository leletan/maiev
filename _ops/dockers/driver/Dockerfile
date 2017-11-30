FROM kubespark/spark-driver:v2.2.0-kubernetes-0.5.0

ENV ASSEMBLY_NAME=maiev-assembly-1.0.jar
ENV JAR_PATH=target/scala-2.11/$ASSEMBLY_NAME

RUN apk upgrade --no-cache && \
    apk add --no-cache bash tini libc6-compat && \
    mkdir -p /opt/spark && \
    mkdir -p /opt/spark/work-dir \
    touch /opt/spark/RELEASE && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd

ADD $JAR_PATH $SPARK_HOME/jars/$ASSEMBLY_NAME



