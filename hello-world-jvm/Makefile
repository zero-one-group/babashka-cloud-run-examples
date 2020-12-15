PROJECT_NAME=cloud-run-experiments
SERVICE_NAME=clojure-jvm-hello-world
DOCKER_NAME=$(SERVICE_NAME)
PORT=8080

build:
	docker build -f Dockerfile -t $(DOCKER_NAME):latest $(PWD)

run: build
	docker run --rm -p 9090:${PORT} -e PORT=${PORT} -t $(DOCKER_NAME):latest

cloud-build:
	gcloud builds submit --tag gcr.io/$(PROJECT_NAME)/$(DOCKER_NAME)

run-deploy:
	gcloud run deploy $(SERVICE_NAME) --image gcr.io/$(PROJECT_NAME)/$(DOCKER_NAME) \
		--no-allow-unauthenticated
