# Google Calendar Events Generator

A Java 23 standalone CLI application that will create recurring weekly Google Calendar events for a specified list of recurring Zoom meetings.

## Prerequisites

1. Java 23 JDK
2. Gradle (or use the included wrapper `./gradlew`)
3. A Google Cloud project with Calendar API enabled
4. OAuth 2.0 client credentials (type "Desktop app") — download `credentials.json` and place it in `src/main/resources/credentials.json`

## Build

### Quick Build
Build the project and create a fat JAR:
```bash
./gradlew clean build shadowJar
```

### Package (Clean + Build + Shadow JAR)
Use the custom `pkg` task to clean, build, and produce the shadow JAR in one command:
```bash
./gradlew pkg
```

The fat JAR will be created at: `build/libs/calendar-events-generator-<version>.jar`

## Run

### Run the Application
```bash
java -Dapp.env=local -jar build/libs/calendar-events-generator-<version>.jar [options]
```

Or for production:
```bash
java -Dapp.env=prod -jar build/libs/calendar-events-generator-<version>.jar [options]
```

### Output

![current output 1.0.0](./assets/images/output-1.0.0.png) 

### Run with Gradle
```bash
./gradlew run --args="[your-arguments]"
```

## Release Management

### Prerequisites

```bash
export CEG_VERSION=1.0.0
export CEG_BUCKET_NAME=my-gcs-bucket
export CEG_FILE=./events.json
export CEG_SCHEMA=./events-schema.v1.json
```

### Create a Release
Convert the current SNAPSHOT version to a release version and build the fat JAR:
```bash
./gradlew release
```

This will:
1. Remove `-SNAPSHOT` from the version in `gradle.properties`
2. Build the fat JAR with the release version
3. Output: `build/libs/calendar-events-generator-<release-version>.jar`

### Bump to Next Development Version
After a release, bump to the next SNAPSHOT version:

```bash
# Bump patch version (e.g., 1.0.0 -> 1.0.1-SNAPSHOT)
./gradlew nextSnapshot

# Bump minor version (e.g., 1.0.0 -> 1.1.0-SNAPSHOT)
./gradlew nextSnapshot -Ptype=minor

# Bump major version (e.g., 1.0.0 -> 2.0.0-SNAPSHOT)
./gradlew nextSnapshot -Ptype=major
```

## Events Configuration Management

### JSON Schema Validation
The application uses a JSON schema to validate the events configuration file. The schema is located at `schema/events-schema.v1.json` and defines the structure for event objects:

```json
{
  "dayOfWeek": "string",
  "time": "string",
  "zoomUrl": "string (uri format)",
  "description": "string"
}
```

All fields are required and no additional properties are allowed.

### Uploading Events to Artifact Registry or Google Cloud Storage

The project supports uploading validated events to **Artifact Registry (preferred)** or **GCS bucket (fallback)**.

#### Quick Start with Helper Script (Recommended)

The easiest way to set up your environment is using the `setup-env.sh` script with your project configuration:

**Quick Setup (Recommended):**
```bash
# One command to set up your entire environment
source ./setup-env.sh

# Then run your commands
make validate  # Test validation
make upload    # Upload to Artifact Registry
```

The `setup-env.sh` file contains your project-specific configuration (edit it to match your setup):
- Project ID (e.g., `node-apps-405212`)
- Artifact Registry URL (e.g., `europe-north2-maven.pkg.dev/PROJECT/REPO`)
- Default version, file paths, and base path

**Alternative: Manual Setup with Helper Script**

If you prefer to specify values each time:

**For Artifact Registry:**
```bash
# Step 1: Get the command to run
make env-ar PROJECT=node-apps-405212 AR_REGISTRY=europe-north2-maven.pkg.dev/node-apps-405212/calendar-events-generator

# Step 2: Copy and run the command shown (this sets all CEG_ variables in your shell)
source ./local/set-ceg-env.sh node-apps-405212 europe-north2-maven.pkg.dev/node-apps-405212/calendar-events-generator

# Step 3: Now you can run make commands (variables persist in your terminal session)
make validate  # Test validation
make upload    # Upload to Artifact Registry
```

**For GCS Bucket (Fallback):**
```bash
make env-gcs BUCKET=my-gcs-bucket
source ./local/set-ceg-env.sh --bucket my-gcs-bucket
make upload
```

**With Custom Version:**
```bash
source ./local/set-ceg-env.sh node-apps-405212 europe-north2-maven.pkg.dev/node-apps-405212/calendar-events-generator --version 2.0.0
make upload
```

The helper script will:
- ✅ Set all 7 CEG_ environment variables automatically
- ✅ Display exactly what was configured
- ✅ Confirm you're ready to upload
- ✅ Keep variables active for your entire terminal session

#### Alternative: Using eval with Make

If you prefer using Make directly:

```bash
# For Artifact Registry
eval $(make -s set-env PROJECT=node-apps-405212 AR_REGISTRY=europe-north2-maven.pkg.dev/node-apps-405212/calendar-events-generator)

# For GCS bucket
eval $(make -s set-env BUCKET=my-gcs-bucket)

# Then run
make validate
make upload
```

#### Using the Shell Script Directly

You can also call the upload script directly with environment variables:

**For Artifact Registry (Primary):**
```bash
CEG_VERSION=1.0.0 \
CEG_FILE=./src/main/resources/events.json \
CEG_PROJECT=my-gcp-project \
CEG_ARTIFACT_REGISTRY=us-central1-maven.pkg.dev/my-gcp-project/calendar-events-generator \
./local/upload-events.sh
```

**For GCS Bucket (Fallback):**
```bash
CEG_VERSION=1.0.0 \
CEG_FILE=./src/main/resources/events.json \
CEG_BUCKET_NAME=my-gcs-bucket \
./local/upload-events.sh
```

**Upload to Both:**
```bash
CEG_VERSION=1.0.0 \
CEG_FILE=./src/main/resources/events.json \
CEG_BUCKET_NAME=my-gcs-bucket \
CEG_PROJECT=my-gcp-project \
CEG_ARTIFACT_REGISTRY=us-central1-maven.pkg.dev/my-gcp-project/calendar-events-generator \
./local/upload-events.sh
```

The script will:
1. Validate the JSON file against the schema using `ajv` and `ajv-formats`
2. Upload to versioned path: `config/VERSION/events.json`
3. Upload to latest path: `config/latest/events.json`

**Environment Variables:**
- `CEG_VERSION` (required): Version to publish (e.g., 1.0.0)
- `CEG_FILE` (required): Path to JSON events file
- `CEG_SCHEMA` (optional): Path to JSON schema (default: ./schema/events-schema.json)
- `CEG_BASE_PATH` (optional): Base path within storage (default: config)
- `CEG_PROJECT` (required for Artifact Registry): GCP project ID
- `CEG_ARTIFACT_REGISTRY` (optional): Artifact Registry path (e.g., us-central1-maven.pkg.dev/PROJECT/REPO)
- `CEG_BUCKET_NAME` (optional): GCS bucket name (fallback)

**Requirements:**
- `gcloud` CLI installed and authenticated
- Node.js with `ajv` and `ajv-formats` modules

#### Available Make Commands

**Environment Setup:**
- `make env-ar PROJECT=... AR_REGISTRY=...` - Show command to set up Artifact Registry environment
- `make env-gcs BUCKET=...` - Show command to set up GCS bucket environment
- `make set-env` - Output export commands (use with eval)

**Build, Validate, and Upload:**
```bash
# Build the Docker image
make build

# Validate events.json locally (no upload)
make validate

# Validate and upload to Artifact Registry or GCS
make upload

# Clean up Docker image
make clean
```

**Available Make Targets:**
- `make build` - Build the Docker image
- `make validate` - Validate events.json locally without uploading
- `make upload` - Validate and upload to Artifact Registry or GCS
- `make clean` - Remove the Docker image
- `make env-ar` - Display Artifact Registry setup command
- `make env-gcs` - Display GCS bucket setup command
- `make set-env` - Output export commands for CEG_ environment variables

**Makefile Variables:**
- `VERSION`: Version to publish (default: 1.0.0)
- `FILE`: JSON data file (default: ./src/main/resources/events.json)
- `SCHEMA`: JSON schema (default: ./schema/events-schema.v1.json)
- `BASE_PATH`: Base path within storage (default: config)
- `PROJECT`: GCP project ID (required for Artifact Registry)
- `AR_REGISTRY`: Artifact Registry path (e.g., us-central1-maven.pkg.dev/PROJECT/REPO)
- `BUCKET`: GCS bucket name (fallback)

**Example Workflows:**

```bash
# Workflow 1: Artifact Registry (Recommended) - Using Helper Script
source ./local/set-ceg-env.sh my-project us-central1-maven.pkg.dev/my-project/calendar-events-generator
make validate  # Test validation
make upload    # Upload to Artifact Registry

# Workflow 2: GCS Bucket (Fallback)
source ./local/set-ceg-env.sh --bucket my-bucket
make validate
make upload

# Workflow 3: Custom version to Artifact Registry
source ./local/set-ceg-env.sh my-project us-central1-maven.pkg.dev/my-project/calendar-events-generator --version 2.1.0
make upload

# Workflow 4: One-time upload without setting environment
make upload PROJECT=my-project AR_REGISTRY=us-central1-maven.pkg.dev/my-project/calendar-events-generator VERSION=1.5.0
```

### Storage Paths

Your files will be organized as follows:

**Artifact Registry:**
```
us-central1-maven.pkg.dev/my-project/calendar-events-generator/
└── config/
    ├── 1.0.0/
    │   └── events.json
    └── latest/
        └── events.json
```

**GCS Bucket:**
```
gs://my-bucket/
└── config/
    ├── 1.0.0/
    │   └── events.json
    └── latest/
        └── events.json
```

### Helper Scripts

#### set-ceg-env.sh
Located at `local/set-ceg-env.sh`, this script simplifies environment variable setup.

**Usage:**
```bash
# Artifact Registry
source ./local/set-ceg-env.sh <project> <ar-registry> [--version VERSION]

# GCS Bucket
source ./local/set-ceg-env.sh --bucket <bucket-name> [--version VERSION]

# Help
source ./local/set-ceg-env.sh --help
```

**Features:**
- Validates inputs before setting variables
- Sets all required CEG_ environment variables
- Displays confirmation of what was set
- Supports custom version flag
- Variables persist in your terminal session
- Clear error messages if inputs are invalid

### Docker Support
A Dockerfile is provided for containerized validation and upload workflows. The image includes:
- Google Cloud SDK
- Node.js with `ajv` and `ajv-formats` for JSON schema validation
- The upload script and schema files

Build the image:
```bash
docker build -t ceg-events-uploader .
```

Run validation and upload to Artifact Registry:
```bash
docker run --rm \
  -e UPLOAD=true \
  -e CEG_VERSION=1.0.0 \
  -e CEG_FILE=./src/main/resources/events.json \
  -e CEG_PROJECT=my-gcp-project \
  -e CEG_ARTIFACT_REGISTRY=us-central1-maven.pkg.dev/my-gcp-project/calendar-events-generator \
  -v ~/.config/gcloud:/root/.config/gcloud \
  -v $(pwd):/workspace \
  ceg-events-uploader
```

Run validation and upload to GCS:
```bash
docker run --rm \
  -e UPLOAD=true \
  -e CEG_VERSION=1.0.0 \
  -e CEG_FILE=./src/main/resources/events.json \
  -e CEG_BUCKET_NAME=my-gcs-bucket \
  -v ~/.config/gcloud:/root/.config/gcloud \
  -v $(pwd):/workspace \
  ceg-events-uploader
```

## CI/CD Pipeline

### GitHub Actions Workflow

The project includes a comprehensive GitHub Actions workflow (`.github/workflows/release.yml`) that automatically runs code quality checks and uploads events.json to Artifact Registry when a version tag is pushed.

#### Workflow Overview

**Trigger:**
```bash
git tag v1.0.0
git push origin v1.0.0
```

**Jobs:**

1. **Qodana Code Quality Check** (runs first)
   - Checks out repository
   - Sets up JDK 23
   - Runs Qodana static analysis
   - Fails pipeline if any critical or high severity issues are found
   - Generates code quality report

2. **Validate and Upload to Artifact Registry** (runs after Qodana passes)
   - Checks out repository
   - Extracts version from Git tag
   - Authenticates to Google Cloud using Workload Identity Federation
   - Builds Docker validation image
   - Validates events.json against schema
   - Uploads to Artifact Registry with both versioned and latest tags
   - Creates deployment summary

**Required GitHub Secrets:**
- `GCP_WORKLOAD_IDENTITY_PROVIDER` - Workload Identity Provider resource name
- `GCP_SERVICE_ACCOUNT` - Service account email for Artifact Registry access
- `GCP_PROJECT_ID` - Your GCP project ID (e.g., node-apps-405212)
- `GCP_ARTIFACT_REGISTRY` - Full Artifact Registry path (e.g., europe-north2-maven.pkg.dev/PROJECT/REPO)
- `QODANA_TOKEN` - (Optional) JetBrains Qodana token for enhanced features

**Environment Variables:**
- `VERSION`: Automatically extracted from Git tag (e.g., v1.0.0 becomes 1.0.0)
- `CEG_FILE`: ./src/main/resources/events.json
- `CEG_SCHEMA`: ./schema/events-schema.v1.json
- `CEG_BASE_PATH`: events
- `CEG_ENABLE_GCS_UPLOAD`: false (Artifact Registry only)

## Code Quality

### Qodana Static Analysis

The project uses JetBrains Qodana for automated code quality checks. Qodana runs both locally and in CI/CD to catch issues early.

#### Running Qodana Locally

**Quick scan (fails on issues):**
```bash
make qodana
```

**Run with interactive report:**
```bash
make qodana-report
# Opens report at http://localhost:8080
```

**Clean Qodana results:**
```bash
make qodana-clean
```

#### Quality Gates

Qodana is configured to fail the build if:
- Any critical severity issues are found (threshold: 0)
- Any high severity issues are found (threshold: 0)
- More than 10 moderate severity issues
- More than 50 low severity issues
- More than 100 info-level issues

Configuration is defined in `qodana.yaml`:
- **Profile**: qodana.recommended
- **Linter**: jetbrains/qodana-jvm-community:2025.2
- **JDK**: 23

#### CI/CD Integration

Qodana runs as the first job in the release pipeline:
1. If Qodana finds critical/high issues → Pipeline fails, no deployment
2. If Qodana passes → Validation and upload proceeds
3. Results are available in GitHub Actions checks

This ensures only high-quality code is released to production.

## Development
