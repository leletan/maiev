# Maiev

To run it locally, do the following:
1. Install and set up minikube v0.22.0 (google for more details) 
2. run `minikube start --kubernetes-version v1.7.5 --cpus 7 --memory 12360 --disk-size 100g`
2. run `make sidecars` (to set up dependencies)
3. run `make publish` (to build and publish all the docker images needed)
4. run `make submit` (to submit the spark job to local minikube cluster)
5. run `kubectl get pods` to find the spark driver pod
6. run `kubectl logs -f <driver-pod-name>` to check the driver log
7. run `kubectl exec -it cockroachdb-0 bash` to log into cockroachdb 
8. run `./cockroach sql --insecure --host=cockroachdb-public -e '<any_sql_query>'` query cockroachDB 
9. have fun
10. run `kubectl delete pod spark-driver-maiev-dev-kafkasourcetest` to stop the spark job