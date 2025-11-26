#!/bin/bash

# VNPay Backend Deployment Script for Cloud Run
# Usage: ./deploy.sh YOUR_TMN_CODE YOUR_HASH_SECRET

set -e

PROJECT_ID="shottinggamepayment"
SERVICE_NAME="vnpay-backend"
REGION="us-central1"

if [ $# -lt 2 ]; then
    echo "Usage: ./deploy.sh YOUR_TMN_CODE YOUR_HASH_SECRET"
    echo ""
    echo "Example:"
    echo "  ./deploy.sh DEMOV210 your_secret_key_here"
    exit 1
fi

VNP_TMNCODE=$1
VNP_HASHSECRET=$2

echo "🚀 Deploying VNPay backend to Cloud Run..."
echo "Project: $PROJECT_ID"
echo "Service: $SERVICE_NAME"
echo ""

# Set project
gcloud config set project $PROJECT_ID

# Build and submit
echo "📦 Building Docker image..."
gcloud builds submit --tag gcr.io/$PROJECT_ID/$SERVICE_NAME

# Deploy
echo "🚢 Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$PROJECT_ID/$SERVICE_NAME \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --set-env-vars VNP_TMNCODE=$VNP_TMNCODE \
  --set-env-vars VNP_HASHSECRET=$VNP_HASHSECRET \
  --set-env-vars VNP_RETURN_URL=yourapp://vnpay-result \
  --set-env-vars VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

# Get service URL
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region $REGION --format 'value(status.url)')

echo ""
echo "✅ Deployment complete!"
echo "📱 Service URL: $SERVICE_URL"
echo ""
echo "Next steps:"
echo "1. Update app/build.gradle.kts with this URL:"
echo "   buildConfigField(\"String\", \"VNPAY_BACKEND_BASE_URL\", \"\\\"$SERVICE_URL/\\\"\")"
echo ""
echo "2. Test the health endpoint:"
echo "   curl $SERVICE_URL/health"

