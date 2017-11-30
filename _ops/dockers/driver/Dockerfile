FROM kubespark/spark-driver:v2.2.0-kubernetes-0.5.0

ENV ASSEMBLY_NAME=maiev-assembly-1.0.jar
ENV JAR_PATH=target/scala-2.11/$ASSEMBLY_NAME

ADD $JAR_PATH /opt/spark/jars/$ASSEMBLY_NAME

