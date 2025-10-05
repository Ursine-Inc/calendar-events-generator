#!/usr/bin/env bash
#
# setup-env.sh - Quick environment setup for your terminal session
#
# Usage: source ./setup-env.sh

# Set your project-specific values here
export CEG_PROJECT="node-apps-405212"
export CEG_ARTIFACT_REGISTRY="europe-north2-maven.pkg.dev/node-apps-405212/calendar-events-generator"

# Set defaults
export CEG_VERSION="${CEG_VERSION:-1.0.0}"
export CEG_FILE="${CEG_FILE:-./src/main/resources/events.json}"
export CEG_SCHEMA="${CEG_SCHEMA:-./schema/events-schema.v1.json}"
export CEG_BASE_PATH="${CEG_BASE_PATH:-events}"
#export CEG_BUCKET_NAME="${CEG_BUCKET_NAME:-}"

echo "✅ Environment configured for node-apps-405212"
echo "   Project: $CEG_PROJECT"
echo "   Registry: $CEG_ARTIFACT_REGISTRY"
echo "   Version: $CEG_VERSION"
echo ""
echo "Ready to run: make validate or make upload"

