#!/usr/bin/env bash
#
# upload-events.sh
#
# Description:
#   Validates a JSON events file against a schema and uploads it
#   to Artifact Registry. Optionally supports GCS bucket as a fallback.
#
# Usage:
#   # For Artifact Registry (default):
#   CEG_VERSION=1.0.0 CEG_FILE=./events.json CEG_PROJECT=my-project \
#   CEG_ARTIFACT_REGISTRY=us-central1-maven.pkg.dev/my-project/my-repo \
#   ./upload-events.sh
#
#   # With optional GCS bucket:
#   CEG_VERSION=1.0.0 CEG_FILE=./events.json CEG_PROJECT=my-project \
#   CEG_ARTIFACT_REGISTRY=us-central1-maven.pkg.dev/my-project/my-repo \
#   CEG_BUCKET_NAME=my-bucket \
#   ./upload-events.sh
#
# Requirements:
#   - bash 4+
#   - gcloud CLI configured for Artifact Registry
#   - Node.js with ajv and ajv-formats modules

set -euo pipefail

# -------------------------------
# Configuration
# -------------------------------
VERSION="${CEG_VERSION:?CEG_VERSION env var must be set (e.g. 1.0.0)}"
FILE="${CEG_FILE:?CEG_FILE env var must be set (path to JSON file)}"
SCHEMA="${CEG_SCHEMA:-./schema/events-schema.json}"
BASE_PATH="${CEG_BASE_PATH:-events}"
PROJECT="${CEG_PROJECT:?CEG_PROJECT env var must be set (GCP project ID)}"
ARTIFACT_REGISTRY="${CEG_ARTIFACT_REGISTRY:?CEG_ARTIFACT_REGISTRY env var must be set (e.g., us-central1-maven.pkg.dev/PROJECT/REPO)}"

# Optional GCS bucket (requires explicit flag to enable)
ENABLE_GCS_UPLOAD="${CEG_ENABLE_GCS_UPLOAD:-false}"
BUCKET="${CEG_BUCKET_NAME:-}"

# -------------------------------
# Check gcloud authentication
# -------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTH_CHECK_SCRIPT="$SCRIPT_DIR/check-gcloud-auth.sh"

if [ -f "$AUTH_CHECK_SCRIPT" ]; then
  if [ -n "$PROJECT" ]; then
    "$AUTH_CHECK_SCRIPT" "$PROJECT"
  else
    "$AUTH_CHECK_SCRIPT"
  fi
elif [ -f "/app/local/check-gcloud-auth.sh" ]; then
  if [ -n "$PROJECT" ]; then
    /app/local/check-gcloud-auth.sh "$PROJECT"
  else
    /app/local/check-gcloud-auth.sh
  fi
else
  echo "[WARNING] Authentication check script not found, skipping check..."
fi

# -------------------------------
# Validate JSON against schema
# -------------------------------
echo "[INFO] Validating $FILE against schema $SCHEMA..."

if [[ "$FILE" == ./* ]]; then
  FILE="/workspace/$FILE"
fi

if [[ "$SCHEMA" == ./* ]]; then
  SCHEMA="/workspace/$SCHEMA"
fi

node -e "
const Ajv = require('ajv');
const addFormats = require('ajv-formats');
const fs = require('fs');

const schema = JSON.parse(fs.readFileSync('$SCHEMA', 'utf8'));
const data = JSON.parse(fs.readFileSync('$FILE', 'utf8'));

const ajv = new Ajv();
addFormats(ajv); // enables 'uri', 'email', etc.

const valid = ajv.validate(schema, data);
if (!valid) {
  console.error(ajv.errors);
  process.exit(1);
}
console.log('Validation passed ✅');
"

echo "[INFO] Validation successful."

# -------------------------------
# Upload to Artifact Registry (primary)
# -------------------------------

if [ "${UPLOAD:-false}" = "true" ]; then

  echo "[INFO] Uploading to Artifact Registry: $ARTIFACT_REGISTRY"

  # Format: LOCATION-TYPE.pkg.dev/PROJECT/REPOSITORY
  LOCATION=$(echo "$ARTIFACT_REGISTRY" | cut -d'-' -f1-2)  # e.g., us-central1
  REPOSITORY=$(echo "$ARTIFACT_REGISTRY" | rev | cut -d'/' -f1 | rev)

  echo "[INFO] Project: $PROJECT, Location: $LOCATION, Repository: $REPOSITORY"

  TEMP_VERSION_FILE="/tmp/events-$VERSION.json"
  TEMP_LATEST_FILE="/tmp/events-latest.json"
  cp "$FILE" "$TEMP_VERSION_FILE"
  cp "$FILE" "$TEMP_LATEST_FILE"

  echo "[INFO] Uploading versioned file ($VERSION) to Artifact Registry..."
  gcloud artifacts generic upload \
    --location="$LOCATION" \
    --repository="$REPOSITORY" \
    --project="$PROJECT" \
    --package="$BASE_PATH" \
    --version="$VERSION" \
    --source="$TEMP_VERSION_FILE"

  echo "[INFO] Uploading as $VERSION-latest (latest alias) to Artifact Registry..."
  gcloud artifacts generic upload \
    --location="$LOCATION" \
    --repository="$REPOSITORY" \
    --project="$PROJECT" \
    --package="$BASE_PATH" \
    --version="$VERSION-latest" \
    --source="$TEMP_LATEST_FILE"

  # Cleanup temp files
  rm -f "$TEMP_VERSION_FILE" "$TEMP_LATEST_FILE"

  echo "[INFO] ✅ Artifact Registry upload complete. Available at:"
  echo "  - https://$ARTIFACT_REGISTRY/$BASE_PATH/$VERSION/"
  echo "  - https://$ARTIFACT_REGISTRY/$BASE_PATH/$VERSION-latest/ (latest version)"

  if [ "$ENABLE_GCS_UPLOAD" = "true" ]; then
    if [ -z "$BUCKET" ]; then
      echo "[ERROR] CEG_ENABLE_GCS_UPLOAD is set to true but CEG_BUCKET_NAME is not set"
      exit 1
    fi

    DEST_VERSION="gs://$BUCKET/$BASE_PATH/$VERSION/events.json"
    DEST_LATEST="gs://$BUCKET/$BASE_PATH/latest/events.json"

    echo ""
    echo "[INFO] Uploading to GCS bucket (optional): $BUCKET"
    echo "[INFO] Uploading $FILE to $DEST_VERSION ..."
    gsutil cp "$FILE" "$DEST_VERSION"

    echo "[INFO] Uploading $FILE to $DEST_LATEST ..."
    gsutil cp "$FILE" "$DEST_LATEST"

    echo "[INFO] ✅ GCS upload complete. Available at:"
    echo "  - $DEST_VERSION"
    echo "  - $DEST_LATEST"
  fi

else
  echo "[INFO] Skipping upload (UPLOAD not set to 'true')"
fi
