ifndef ENV
ENV=dev
endif

ifndef JOB
JOB=KafkaSourceTest
endif

publish_artifact:
	bash ./_ops/publish_artifact.sh $(ENV)

deploy:
	bash ./_ops/deploy.sh $(JOB)

submit:
	bash ./_ops/submit.sh $(ENV) $(JOB) $(VER)

sidecars:
	bash ./_ops/sidecars.sh $(ENV)