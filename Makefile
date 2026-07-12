IMAGE_NAME     ?= order-monolith
IMAGE_TAG      ?= 0.1.0
CONTAINER_NAME ?= order-app
PORT           ?= 8080

# host.docker.internal lets the container reach a Postgres running on the
# host (e.g. via Docker Desktop). Override for a real deployment target.
DB_URL      ?= jdbc:postgresql://host.docker.internal:5432/orderdb
DB_USER     ?= orderapp
DB_PASSWORD ?= orderapp

.PHONY: build deploy undeploy redeploy

build:
	docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .

deploy:
	docker run -d \
		--name $(CONTAINER_NAME) \
		-p $(PORT):8080 \
		-e DB_URL=$(DB_URL) \
		-e DB_USER=$(DB_USER) \
		-e DB_PASSWORD=$(DB_PASSWORD) \
		$(IMAGE_NAME):$(IMAGE_TAG)

undeploy:
	docker stop $(CONTAINER_NAME)
	docker rm $(CONTAINER_NAME)

redeploy: undeploy build deploy
