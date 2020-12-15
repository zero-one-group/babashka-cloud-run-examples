PROJECT_NAME=cloud-run-experiments
SERVICE_NAME=babashka-spend-aggregator-v2
DOCKER_NAME=$(SERVICE_NAME)
PORT=8080

APP_URL=localhost:$(PORT)

build:
	docker build -f Dockerfile -t $(DOCKER_NAME):latest $(PWD)

run: build
	docker run --rm -p $(PORT):${PORT} -e PORT=${PORT} -t $(DOCKER_NAME):latest

idr-spends:
	curl -s -X POST \
		-H "Content-Type: application/json" \
		-d "@sample.json" \
		$(APP_URL)/idr-spends

aggregate-spends:
	curl -s -X POST \
		-H "Content-Type: application/json" \
		-d "@sample.json" \
		$(APP_URL)/aggregate-spends

cloud-build:
	gcloud builds submit --tag gcr.io/$(PROJECT_NAME)/$(DOCKER_NAME)

run-deploy:
	gcloud run deploy $(SERVICE_NAME) --image gcr.io/$(PROJECT_NAME)/$(DOCKER_NAME) \
		--no-allow-unauthenticated
