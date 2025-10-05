#!/usr/bin/env bash
#
# set-ceg-env.sh
#
# Description:
#   Helper script to set CEG_ environment variables in the current shell.
#   This script must be sourced, not executed.
#
# Usage:
#   source ./local/set-ceg-env.sh my-gcp-project us-central1-maven.pkg.dev/my-gcp-project/calendar-events-generator
#   source ./local/set-ceg-env.sh --bucket my-gcs-bucket
#   source ./local/set-ceg-env.sh my-gcp-project us-central1-maven.pkg.dev/my-gcp-project/calendar-events-generator --version 2.0.0

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "❌ Error: This script must be sourced, not executed."
    echo "Usage: source $0 <project> <ar-registry> [--version VERSION]"
    echo "   or: source $0 --bucket <bucket-name> [--version VERSION]"
    exit 1
fi

VERSION="1.0.0"
PROJECT=""
AR_REGISTRY=""
BUCKET=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --bucket)
            BUCKET="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: source $0 <project> <ar-registry> [--version VERSION]"
            echo "   or: source $0 --bucket <bucket-name> [--version VERSION]"
            echo ""
            echo "Examples:"
            echo "  source $0 my-project us-central1-maven.pkg.dev/my-project/calendar-events-generator"
            echo "  source $0 --bucket my-gcs-bucket"
            echo "  source $0 my-project us-central1-maven.pkg.dev/my-project/repo --version 2.0.0"
            return 0
            ;;
        *)
            if [[ -z "$PROJECT" ]]; then
                PROJECT="$1"
            elif [[ -z "$AR_REGISTRY" ]]; then
                AR_REGISTRY="$1"
            fi
            shift
            ;;
    esac
done

if [[ -z "$BUCKET" && -z "$AR_REGISTRY" ]]; then
    echo "❌ Error: Either provide PROJECT and AR_REGISTRY, or use --bucket"
    echo "Usage: source $0 <project> <ar-registry> [--version VERSION]"
    echo "   or: source $0 --bucket <bucket-name> [--version VERSION]"
    return 1
fi

if [[ -n "$AR_REGISTRY" && -z "$PROJECT" ]]; then
    echo "❌ Error: PROJECT is required when using AR_REGISTRY"
    return 1
fi

export CEG_VERSION="$VERSION"
export CEG_FILE="./src/main/resources/events.json"
export CEG_SCHEMA="./schema/events-schema.v1.json"
export CEG_BASE_PATH="events"
export CEG_PROJECT="$PROJECT"
export CEG_ARTIFACT_REGISTRY="$AR_REGISTRY"
export CEG_BUCKET_NAME="$BUCKET"

echo "✅ CEG environment variables set:"
echo "   CEG_VERSION=$CEG_VERSION"
echo "   CEG_FILE=$CEG_FILE"
echo "   CEG_SCHEMA=$CEG_SCHEMA"
echo "   CEG_BASE_PATH=$CEG_BASE_PATH"

if [[ -n "$PROJECT" ]]; then
    echo "   CEG_PROJECT=$CEG_PROJECT"
fi

if [[ -n "$AR_REGISTRY" ]]; then
    echo "   CEG_ARTIFACT_REGISTRY=$CEG_ARTIFACT_REGISTRY"
    echo ""
    echo "🚀 Ready to upload to Artifact Registry"
fi

if [[ -n "$BUCKET" ]]; then
    echo "   CEG_BUCKET_NAME=$CEG_BUCKET_NAME"
    echo ""
    echo "🚀 Ready to upload to GCS Bucket"
fi

echo ""
echo "Now you can run: make validate or make upload"

