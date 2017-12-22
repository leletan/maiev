# Maiev

To run it locally, do the following:
1. Install and set up minikube v0.22.3 (google for more details) 
2. run `minikube start --kubernetes-version v1.7.5 --cpus 7 --memory 12360 --disk-size 100g`
2. run `make sidecars` (to set up dependencies)
3. run `make publish` (to build and publish all the docker images needed)
4. run `make submit` (to submit the job to local minikube cluster)
5. run `kubectl logs -f spark-driver-maiev-dev-kafkasourcetest` to check the driver log
6. send some message to kafka by log into the kafka pod and run `./opt/kafka_2.11-0.10.1.0/bin/kafka-console-producer.sh  --broker-list localhost:9092 --topic topic1`
7. have fun