.DEFAULT_GOAL := help
.NOTPARALLEL:

APP ?= actuator-one
DOCKER_APP ?= actuator-one-docker
NATIVE_APP ?= actuator-one-binary
ARTIFACT ?= $(shell sed -n '/<\/parent>/,$$s/.*<artifactId>\([^<]*\)<\/artifactId>.*/\1/p' pom.xml | head -n 1)
VERSION ?= $(shell sed -n '/<\/parent>/,$$s/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -n 1)
JAR ?= target/$(ARTIFACT)-$(VERSION).jar
IMAGE ?= tapi-docker-prod-local.usw1.packages.broadcom.com/tapi-docker-prod-local/actuator-one
GRAALVM_URL ?=
BASE_IMAGE ?=
SMOKE_SCHEME ?= https
REQUIRED_LINKS ?= health info
# Extra curl flags for smoke, e.g. CURL_OPTS=-k (or --cacert file) for a foundation with a private CA
CURL_OPTS ?=

.PHONY: help check-env build test package run clean push deploy status logs delete smoke docker-login docker-build docker-push docker-preflight docker-deploy smoke-docker native-build native-check native-deploy smoke-native

help: ## List available targets
	@printf '%s\n' \
	  'help        List available targets' \
	  'check-env   Check credentials and Cloud Foundry target' \
	  'build       Build the application without running tests' \
	  'test        Run the test suite' \
	  'package     Clean, test, and package the application' \
	  'run         Run the application locally' \
	  'clean       Remove build output' \
	  'push        Push the existing jar to Cloud Foundry' \
	  'deploy      Check environment, package, then push' \
	  'status      Show Cloud Foundry app status' \
	  'logs        Show recent Cloud Foundry logs' \
	  'delete      Delete the Cloud Foundry app and route' \
	  'smoke       Check app endpoints (BASE_URL=http://localhost:18081 skips cf lookup)' \
	  'docker-login Authenticate to the Docker registry with usw1-login' \
	  'docker-build Build the local JVM Docker image for linux/amd64' \
	  'docker-push  Push the tagged Docker image to the registry' \
	  'docker-preflight Require the diego_docker feature flag to be enabled' \
	  'docker-deploy Check environment, package, preflight, build, push, then deploy Docker app' \
	  'smoke-docker Check endpoints for actuator-one-docker' \
	  'native-build Build the linux/amd64 GraalVM native executable' \
	  'native-check Check native executable shared library dependencies' \
	  'native-deploy Check environment, package, build, then deploy binary app' \
	  'smoke-native Check endpoints for actuator-one-binary'

check-env: ## Check required credentials and Cloud Foundry target
	@missing=0; \
	if [ -z "$${USW1_ARTIFACTORY_USER:-}" ]; then echo 'Missing required environment variable: USW1_ARTIFACTORY_USER'; missing=1; fi; \
	if [ -z "$${USW1_ARTIFACTORY_PASSWORD:-}" ]; then echo 'Missing required environment variable: USW1_ARTIFACTORY_PASSWORD'; missing=1; fi; \
	if ! command -v cf >/dev/null 2>&1; then echo 'Missing required command: cf'; missing=1; \
	elif ! cf target >/dev/null 2>&1; then echo 'cf target failed; log in and select a target'; missing=1; fi; \
	[ "$$missing" -eq 0 ]

build: ## Build without running tests
	./mvnw clean package -DskipTests

test: ## Run the test suite
	./mvnw test

package: ## Clean, test, and package the application
	./mvnw clean package

run: ## Run the application locally
	./mvnw spring-boot:run

clean: ## Remove build output
	./mvnw clean

push: ## Push the existing jar to Cloud Foundry
	@test -f "$(JAR)" || { echo 'run make build'; exit 1; }
	cf push $(APP) -f manifest.yml -p $(JAR)

deploy: check-env package push ## Check environment, package, then push

docker-login: ## Authenticate to the Docker registry with usw1-login
	zsh -ic usw1-login

docker-build: ## Build the local JVM Docker image for linux/amd64
	@test -f "$(JAR)" || { echo 'run make build'; exit 1; }
	docker build --platform linux/amd64 --build-arg JAR_FILE=$(JAR) -t $(IMAGE):$(VERSION) .

docker-push: ## Push the tagged Docker image to the registry
	@docker push $(IMAGE):$(VERSION) || { echo 'docker push failed; run make docker-login and retry'; exit 1; }

docker-preflight: ## Require the diego_docker feature flag to be enabled
	@flags=$$(cf feature-flags) || { echo 'Unable to read Cloud Foundry feature flags'; exit 1; }; \
	printf '%s\n' "$$flags"; \
	printf '%s\n' "$$flags" | awk '$$1 == "diego_docker" && $$2 == "enabled" { enabled=1 } END { exit !enabled }' || { echo 'Cloud Foundry feature flag diego_docker must be enabled'; exit 1; }

# If cf push fails, image $(VERSION) is already pushed and actuator-one-docker may be stopped or crashed.
# Inspect with cf logs actuator-one-docker --recent and cf app actuator-one-docker, then fix and rerun make docker-deploy.
# Restore an older image with the target's cf push flags and a previous tag; make delete APP=$(DOCKER_APP) removes the Docker app entirely.
docker-deploy: check-env package docker-preflight docker-build docker-push ## Check environment, package, preflight, build, push, then deploy Docker app
	@CF_DOCKER_PASSWORD="$$USW1_ARTIFACTORY_PASSWORD" cf push $(DOCKER_APP) -f manifest-docker.yml --docker-image $(IMAGE):$(VERSION) --docker-username $$USW1_ARTIFACTORY_USER

smoke-docker: ## Check endpoints for actuator-one-docker
	$(MAKE) smoke APP=actuator-one-docker

native-build: ## Build the linux/amd64 GraalVM native executable
	@test -f "$$HOME/.m2/settings.xml" || { echo "Missing Maven settings file: $$HOME/.m2/settings.xml"; exit 1; }
	@if ! docker info >/dev/null 2>&1; then echo 'Docker is unavailable; start Docker and retry native-build'; exit 1; fi
	@if [ -z "$${USW1_ARTIFACTORY_USER:-}" ]; then echo 'Missing required environment variable: USW1_ARTIFACTORY_USER'; exit 1; fi
	@if [ -z "$${USW1_ARTIFACTORY_PASSWORD:-}" ]; then echo 'Missing required environment variable: USW1_ARTIFACTORY_PASSWORD'; exit 1; fi
	mkdir -p target/native
	@set -eu; set --; \
	  if [ -n "$(GRAALVM_URL)" ]; then set -- "$$@" --build-arg GRAALVM_URL="$(GRAALVM_URL)"; fi; \
	  if [ -n "$(BASE_IMAGE)" ]; then set -- "$$@" --build-arg BASE_IMAGE="$(BASE_IMAGE)"; fi; \
	  docker buildx build --platform linux/amd64 \
	    --secret id=m2settings,src=$$HOME/.m2/settings.xml \
	    --secret id=usw1_user,env=USW1_ARTIFACTORY_USER \
	    --secret id=usw1_pass,env=USW1_ARTIFACTORY_PASSWORD \
	    "$$@" --output type=local,dest=target/native -f Dockerfile.native .

native-check: ## Check native executable shared library dependencies
	docker run --rm --platform linux/amd64 -v $(CURDIR)/target/native:/n:ro ubuntu:22.04 sh -c 'ldd /n/actone > /tmp/ldd.out 2>&1 || { cat /tmp/ldd.out; exit 1; }; cat /tmp/ldd.out; ! grep -q "not found" /tmp/ldd.out'

native-deploy: check-env package native-build ## Check environment, package, build, then deploy binary app
	cf push $(NATIVE_APP) -f manifest-binary.yml

smoke-native: ## Check endpoints for actuator-one-binary
	$(MAKE) smoke APP=actuator-one-binary REQUIRED_LINKS="health info"

status: ## Show Cloud Foundry app status
	cf app $(APP)

logs: ## Show recent Cloud Foundry logs
	cf logs $(APP) --recent

delete: ## Delete the Cloud Foundry app and route
	cf delete $(APP) -f -r

smoke: ## Check app endpoints (BASE_URL override skips cf route lookup)
	@set -u; \
	if ! command -v jq >/dev/null 2>&1; then echo 'smoke requires jq on PATH' >&2; exit 1; fi; \
	if [ -n "$(BASE_URL)" ]; then base="$(BASE_URL)"; \
	else \
	  app_output=$$(cf app "$(APP)") || { echo 'Unable to resolve route: cf app failed' >&2; exit 1; }; \
	  route=$$(printf '%s\n' "$$app_output" | sed -n 's/^[[:space:]]*routes:[[:space:]]*//p' | head -n 1 | cut -d, -f1 | sed 's/[[:space:]]*$$//'); \
	  if [ -z "$$route" ]; then echo 'Unable to resolve route: no routes found in cf app output' >&2; exit 1; fi; \
	  base="$(SMOKE_SCHEME)://$$route"; \
	fi; \
	base=$${base%/}; \
	tmp=$$(mktemp -d) || exit 1; \
	trap 'rm -rf "$$tmp"' EXIT HUP INT TERM; \
	failures=0; \
	check() { \
	  path="$$1"; kind="$$2"; body="$$tmp/body"; reason=''; \
	  if response=$$(curl -sS --max-time 20 $(CURL_OPTS) -o "$$body" -w '%{http_code}|%{content_type}' "$$base$$path"); then \
	    code=$${response%%|*}; content_type=$${response#*|}; \
	    if [ "$$code" != 200 ]; then reason="expected HTTP 200, got $$code"; \
	    else \
	      case "$$kind" in \
	        html) case "$$content_type" in *text/html*) ;; *) reason="Content-Type is not text/html ($$content_type)";; esac ;; \
	        hello) printf 'Hello World!' > "$$tmp/expected"; cmp -s "$$tmp/expected" "$$body" || reason='body is not exactly Hello World!' ;; \
	        missions) jq -e 'type == "array" and length > 0 and all(.[]; has("id") and has("name") and has("crew") and has("launchDate") and has("status")) and any(.[]; .id == "apollo-11")' "$$body" >/dev/null 2>&1 || reason='mission array has missing fields or does not contain apollo-11' ;; \
	        mission) jq -e 'type == "object" and has("id") and has("name") and has("crew") and has("launchDate") and has("status")' "$$body" >/dev/null 2>&1 || reason='missing required mission fields' ;; \
	        menu) jq -e 'type == "array" and length > 0 and all(.[]; has("id") and has("name") and has("priceUsd") and has("category")) and any(.[]; .id == "espresso")' "$$body" >/dev/null 2>&1 || reason='menu array has missing fields or does not contain espresso' ;; \
	        orders) jq -e 'type == "array" and length > 0 and all(.[]; has("id") and has("customer") and has("items") and has("totalUsd") and has("status"))' "$$body" >/dev/null 2>&1 || reason='orders must be non-empty and contain required fields' ;; \
	        weather) jq -e 'type == "array" and length > 0 and all(.[]; has("city") and has("station") and has("tempC") and has("conditions")) and any(.[]; .city == "oslo")' "$$body" >/dev/null 2>&1 || reason='weather array has missing fields or does not contain oslo' ;; \
	        oslo) jq -e 'type == "object" and has("city") and has("station") and has("tempC") and has("conditions")' "$$body" >/dev/null 2>&1 || reason='missing required weather fields' ;; \
	        runtime) jq -e 'type == "object" and has("javaVersion") and has("javaVendor") and has("systemProperties")' "$$body" >/dev/null 2>&1 || reason='missing required runtime fields' ;; \
	        time) jq -e 'type == "object" and has("utc")' "$$body" >/dev/null 2>&1 || reason='missing utc field' ;; \
	        routes) jq -e 'type == "array" and length > 0 and all(.[]; has("path") and has("methods"))' "$$body" >/dev/null 2>&1 || reason='routes must be non-empty and contain path and methods' ;; \
	        actuator) for link in $(REQUIRED_LINKS); do jq -e --arg name "$$link" '._links | has($$name)' "$$body" >/dev/null 2>&1 || reason="missing actuator link $$link"; done ;; \
	        health) jq -e '.status == "UP"' "$$body" >/dev/null 2>&1 || reason='status is not UP' ;; \
	      esac; \
	    fi; \
	  else reason='request failed'; fi; \
	  if [ -n "$$reason" ]; then echo "FAIL $$path ($$reason)"; failures=1; \
	  else echo "PASS $$path"; fi; \
	}; \
	check / html; \
	check /hello hello; \
	check /api/missions missions; \
	check /api/missions/apollo-11 mission; \
	check /api/menu menu; \
	check /api/orders orders; \
	check /api/weather weather; \
	check /api/weather/oslo oslo; \
	check /api/runtime runtime; \
	check /api/time time; \
	check /api/routes routes; \
	check /actuator actuator; \
	check /actuator/health health; \
	exit "$$failures"
