#!/bin/bash

# ==============================================================================
# Cloud Run Deployment Script
# ==============================================================================
# Ensure you are authenticated with Google Cloud CLI before running this script
# Run: gcloud auth login
#      gcloud config set project [YOUR_PROJECT_ID]
# ==============================================================================

# Variables (Please fill these in or pass them as environment variables)
PROJECT_ID=${GCP_PROJECT_ID:-"speakvn-sp26se049-v2"}
REGION=${GCP_REGION:-"asia-southeast1"}
SERVICE_NAME="speakvn-backend"
IMAGE_TAG="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"

echo "Deploying ${SERVICE_NAME} to Google Cloud Run in project ${PROJECT_ID}..."

# 1. Build the Docker image using Google Cloud Build
echo "Building the Docker image..."
gcloud builds submit --tag ${IMAGE_TAG}

if [ $? -ne 0 ]; then
  echo "Error: Docker image build failed."
  exit 1
fi

# 2. Deploy to Cloud Run
# Uncomment and configure the --set-env-vars section if you need to pass database credentials.
# Note: For Cloud SQL you often need to use `--add-cloudsql-instances=[INSTANCE_CONNECTION_NAME]`
echo "Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_TAG} \
  --region ${REGION} \
  --platform managed \
  --port 8082 \
  --allow-unauthenticated \
  --min-instances 1



if [ $? -eq 0 ]; then
  echo "Deployment successful!"
else
  echo "Deployment failed."
  exit 1
fi
