#!/usr/bin/env bash
#
# check-gcloud-auth.sh
#
# Description:
#   Checks if gcloud is authenticated and prompts for login if needed.
#   Also validates that the correct project is set if provided.
#   Verifies that required Google Cloud APIs are enabled.
#
# Usage:
#   ./local/check-gcloud-auth.sh [PROJECT_ID]

set -euo pipefail

PROJECT_ID="${1:-}"

echo "[INFO] Checking gcloud authentication..."

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "[ERROR] gcloud CLI is not installed. Please install it first:"
    echo "  https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if user is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo "[ERROR] No active gcloud authentication found."
    echo ""
    echo "Please run one of the following commands to authenticate:"
    echo ""
    echo "  For user authentication:"
    echo "    gcloud auth login"
    echo ""
    echo "  For service account authentication:"
    echo "    gcloud auth activate-service-account --key-file=path/to/key.json"
    echo ""
    echo "  To use application default credentials:"
    echo "    gcloud auth application-default login"
    echo ""
    exit 1
fi

ACTIVE_ACCOUNT=$(gcloud auth list --filter=status:ACTIVE --format="value(account)" | head -n1)
echo "[INFO] ✅ Authenticated as: $ACTIVE_ACCOUNT"

# Check if a project is provided and set
if [ -n "$PROJECT_ID" ]; then
    CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null || echo "")

    if [ -z "$CURRENT_PROJECT" ]; then
        echo "[INFO] Setting gcloud project to: $PROJECT_ID"
        gcloud config set project "$PROJECT_ID" &> /dev/null
        echo "[INFO] ✅ Project set to: $PROJECT_ID"
    elif [ "$CURRENT_PROJECT" != "$PROJECT_ID" ]; then
        echo "[WARNING] Current gcloud project is '$CURRENT_PROJECT' but script expects '$PROJECT_ID'"
        echo "[INFO] Setting gcloud project to: $PROJECT_ID"
        gcloud config set project "$PROJECT_ID" &> /dev/null
        echo "[INFO] ✅ Project set to: $PROJECT_ID"
    else
        echo "[INFO] ✅ Project already set to: $PROJECT_ID"
    fi

    # Check if API check should be skipped
    if [ "${SKIP_API_CHECK:-false}" = "true" ]; then
        echo "[INFO] ⚠️  Skipping API check (SKIP_API_CHECK=true)"
        echo "[INFO] Assuming all required APIs are enabled"
    else
        # Check if required APIs are enabled
        echo "[INFO] Checking required Google Cloud APIs..."

        REQUIRED_APIS=(
            "artifactregistry.googleapis.com"
            "storage-api.googleapis.com"
            "storage-component.googleapis.com"
        )

        MISSING_APIS=()
        PERMISSION_ERROR=false

        for api in "${REQUIRED_APIS[@]}"; do
            API_CHECK=$(gcloud services list --enabled --project="$PROJECT_ID" --filter="name:$api" --format="value(name)" 2>&1)

            if echo "$API_CHECK" | grep -q "Permission denied\|does not have permission"; then
                echo "[WARNING] ⚠️  Permission denied checking API: $api"
                PERMISSION_ERROR=true
                break
            elif echo "$API_CHECK" | grep -q "$api"; then
                echo "[INFO] ✅ API enabled: $api"
            else
                echo "[WARNING] ⚠️  API not enabled: $api"
                MISSING_APIS+=("$api")
            fi
        done

        if [ "$PERMISSION_ERROR" = true ]; then
            echo ""
            echo "[WARNING] You don't have permission to check APIs for project '$PROJECT_ID'"
            echo ""
            echo "Your account: $ACTIVE_ACCOUNT"
            echo "Current project: $PROJECT_ID"
            echo ""
            echo "This can happen even for project owners due to organization policies."
            echo ""
            echo "To skip this check and proceed anyway, set:"
            echo "  export SKIP_API_CHECK=true"
            echo ""
            echo "Or enable the APIs in the Google Cloud Console:"
            echo "  https://console.cloud.google.com/apis/library?project=$PROJECT_ID"
            echo ""
            echo "Required APIs:"
            echo "  - Artifact Registry API"
            echo "  - Cloud Storage API"
            echo "  - Cloud Storage JSON API"
            echo ""
            exit 1
        fi

        if [ ${#MISSING_APIS[@]} -gt 0 ]; then
            echo ""
            echo "[ERROR] The following required APIs are not enabled for project '$PROJECT_ID':"
            for api in "${MISSING_APIS[@]}"; do
                echo "  - $api"
            done
            echo ""
            echo "To enable these APIs, run the following commands:"
            echo ""
            for api in "${MISSING_APIS[@]}"; do
                echo "  gcloud services enable $api --project=$PROJECT_ID"
            done
            echo ""
            echo "Or enable all at once:"
            echo "  gcloud services enable ${MISSING_APIS[*]} --project=$PROJECT_ID"
            echo ""
            echo "If you don't have permission, enable them in the Google Cloud Console:"
            echo "  https://console.cloud.google.com/apis/library?project=$PROJECT_ID"
            echo ""
            exit 1
        fi

        echo "[INFO] ✅ All required APIs are enabled"
    fi
fi

echo "[INFO] gcloud authentication check complete."
