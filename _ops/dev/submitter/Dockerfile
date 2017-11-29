FROM kubespark/spark-base:v2.2.0-kubernetes-0.3.0

ADD .kube/config /root/.kube/config

RUN mkdir -p /root/.minikube
ADD .kube/ca.crt /root/.minikube/ca.crt
ADD .kube/ca.key /root/.minikube/ca.key

WORKDIR $SPARK_HOME