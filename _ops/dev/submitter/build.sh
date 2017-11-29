#!/bin/bash -ex

cp ~/.minikube/apiserver.key .kube/ca.key
cp ~/.minikube/apiserver.crt .kube/ca.crt
sed "s~\${MINIKUBE_IP}~${KUBEMASTER_IP}~g" <.kube/config.template >.kube/config

docker build -t leletan/spark-k8s-submitter:dev .

rm .kube/ca.key
rm .kube/ca.crt
rm .kube/config

