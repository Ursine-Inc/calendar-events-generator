# ============================================================
#  Makefile for events.json validation and upload to Artifact Registry/GCS
# ============================================================
#  Usage:
#    make build           - Build the Docker image
#    make validate        - Validate events.json locally
#    make upload          - Validate and upload to Artifact Registry (or GCS as fallback)
#    make clean           - Remove Docker image
#    make set-env         - Output export commands for CEG_ environment variables
#
#  Variables (override with `make VAR=value`):
#    VERSION     - Version to publish (default: 1.0.0)
#    FILE        - JSON data file (default: ./src/main/resources/events.json)
#    SCHEMA      - JSON schema (default: ./schema/events-schema.v1.json)
#    BASE_PATH   - Base path within storage (default: config)
#    PROJECT     - GCP project ID (required for Artifact Registry)
#    AR_REGISTRY - Artifact Registry path (e.g., us-central1-maven.pkg.dev/PROJECT/REPO)
#    BUCKET      - GCS bucket name (fallback if AR_REGISTRY not set)
# ============================================================

IMAGE_NAME := ceg-events-uploader
CONFIG_DIR := $(HOME)/.config/gcloud
WORKDIR := $(shell pwd)

# Default values
VERSION ?= 1.0.0
FILE ?= ./src/main/resources/events.json
SCHEMA ?= ./schema/events-schema.v1.json
BASE_PATH ?= config
PROJECT ?=
AR_REGISTRY ?=
BUCKET ?=
SKIP_API_CHECK ?= false

# Override with environment variables if set
ifdef CEG_VERSION
    VERSION := $(CEG_VERSION)
endif
ifdef CEG_FILE
    FILE := $(CEG_FILE)
endif
ifdef CEG_SCHEMA
    SCHEMA := $(CEG_SCHEMA)
endif
ifdef CEG_BASE_PATH
    BASE_PATH := $(CEG_BASE_PATH)
endif
ifdef CEG_PROJECT
    PROJECT := $(CEG_PROJECT)
endif
ifdef CEG_ARTIFACT_REGISTRY
    AR_REGISTRY := $(CEG_ARTIFACT_REGISTRY)
endif
ifdef CEG_BUCKET_NAME
    BUCKET := $(CEG_BUCKET_NAME)
endif

# ============================================================
# Set environment variables for use in current shell
# Usage:
#   make set-env PROJECT=my-project AR_REGISTRY=us-central1-maven.pkg.dev/my-project/my-repo
#   make set-env BUCKET=my-bucket  # For GCS fallback
# ============================================================
.PHONY: set-env
set-env:
	@echo "🔧 Setting up environment variables..."
	@echo ""
	@if [ -n "$(PROJECT)" ] && [ -n "$(AR_REGISTRY)" ]; then \
		echo "Run this command to set your environment for Artifact Registry:"; \
		echo ""; \
		echo "  source ./local/set-ceg-env.sh $(PROJECT) $(AR_REGISTRY)"; \
	elif [ -n "$(BUCKET)" ]; then \
		echo "Run this command to set your environment for GCS Bucket:"; \
		echo ""; \
		echo "  source ./local/set-ceg-env.sh --bucket $(BUCKET)"; \
	else \
		echo "Run this command to set your environment (using setup-env.sh):"; \
		echo ""; \
		echo "  source ./setup-env.sh"; \
		echo ""; \
		echo "Or use the helper script directly:"; \
		echo "  source ./local/set-ceg-env.sh <project> <ar-registry>"; \
		echo "  source ./local/set-ceg-env.sh --bucket <bucket>"; \
	fi
	@echo ""

# ============================================================
# Helper: Display instructions to source the environment script
# Usage (Artifact Registry):
#   make env-ar PROJECT=my-project AR_REGISTRY=us-central1-maven.pkg.dev/my-project/my-repo
# Usage (GCS Bucket):
#   make env-gcs BUCKET=my-bucket
# ============================================================
.PHONY: env-ar
env-ar:
	@echo "🔧 Setting up environment for Artifact Registry..."
	@echo ""
	@echo "Run this command to set your environment variables:"
	@echo ""
	@echo "  source ./local/set-ceg-env.sh $(PROJECT) $(AR_REGISTRY)"
	@echo ""

.PHONY: env-gcs
env-gcs:
	@echo "🔧 Setting up environment for GCS Bucket..."
	@echo ""
	@echo "Run this command to set your environment variables:"
	@echo ""
	@echo "  source ./local/set-ceg-env.sh --bucket $(BUCKET)"
	@echo ""

# --- Docker Build ---
.PHONY: build
build:
	@echo "🛠️  Building Docker image..."
	docker build -t $(IMAGE_NAME) .

# --- Validate only (no upload) ---
.PHONY: validate
validate: build
	@echo "🔍 Validating events.json locally..."
	docker run --rm \
		-e UPLOAD=false \
		-e CEG_FILE=$(FILE) \
		-e CEG_SCHEMA=$(SCHEMA) \
		-e CEG_VERSION=$(VERSION) \
		-e CEG_BUCKET_NAME=$(BUCKET) \
		-e CEG_PROJECT=$(PROJECT) \
		-e CEG_ARTIFACT_REGISTRY=$(AR_REGISTRY) \
		-e CEG_BASE_PATH=$(BASE_PATH) \
		-e SKIP_API_CHECK=$(SKIP_API_CHECK) \
		-v $(CONFIG_DIR):/root/.config/gcloud \
		-v $(WORKDIR):/workspace \
		$(IMAGE_NAME)

# --- Validate + upload to Artifact Registry or GCS ---
.PHONY: upload
upload: build
	@echo "☁️  Validating and uploading events.json..."
	docker run --rm \
		-e UPLOAD=true \
		-e CEG_FILE=$(FILE) \
		-e CEG_SCHEMA=$(SCHEMA) \
		-e CEG_VERSION=$(VERSION) \
		-e CEG_BUCKET_NAME=$(BUCKET) \
		-e CEG_PROJECT=$(PROJECT) \
		-e CEG_ARTIFACT_REGISTRY=$(AR_REGISTRY) \
		-e CEG_BASE_PATH=$(BASE_PATH) \
		-e SKIP_API_CHECK=$(SKIP_API_CHECK) \
		-v $(CONFIG_DIR):/root/.config/gcloud \
		-v $(WORKDIR):/workspace \
		$(IMAGE_NAME)

# --- Clean up Docker image ---
.PHONY: clean
clean:
	@echo "🧹 Cleaning up Docker image..."
	-docker rmi $(IMAGE_NAME) || true

# ============================================================
# Qodana Code Quality Checks
# ============================================================
.PHONY: qodana
qodana:
	@echo "🔍 Running Qodana code quality analysis..."
	@echo "This will fail the build if any critical or high severity issues are found."
	docker run --rm \
		-v $(WORKDIR):/data/project \
		-v $(WORKDIR)/.qodana:/data/results \
		jetbrains/qodana-jvm-community:2025.2 \
		--fail-threshold 0

.PHONY: qodana-report
qodana-report:
	@echo "📊 Running Qodana and opening report..."
	docker run --rm \
		-v $(WORKDIR):/data/project \
		-v $(WORKDIR)/.qodana:/data/results \
		-p 8080:8080 \
		jetbrains/qodana-jvm-community:2025.2 \
		--show-report

.PHONY: qodana-clean
qodana-clean:
	@echo "🧹 Cleaning Qodana results..."
	rm -rf .qodana
