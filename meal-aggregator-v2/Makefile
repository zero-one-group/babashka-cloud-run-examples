PROJECT_NAME=cloud-run-experiments
SERVICE_NAME=babashka-meal-aggregator-v2
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

fill-calories:
	curl -s -X GET \
		-H "Content-Type: application/json" \
		-d "@sample.json" \
		localhost:8080/fill-calories

aggregate-meals:
	curl -s -X GET \
		-H "Content-Type: application/json" \
		-d "@sample.json" \
		localhost:8080/aggregate-meals
