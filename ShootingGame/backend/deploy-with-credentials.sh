#!/bin/bash

# VNPay Backend Deployment Script - Ready to Deploy
# Your VNPay credentials are already configured below

set -e

PROJECT_ID="shottinggamepayment"
SERVICE_NAME="vnpay-backend"
REGION="us-central1"

# Your VNPay Sandbox Credentials (from email)
VNP_TMNCODE="6ZLM2DWT"
VNP_HASHSECRET="3W7PP6S7VEJSTIG8YVUS9B24FLZVWVDN"

echo "🚀 Deploying VNPay backend to Cloud Run..."
echo "Project: $PROJECT_ID"
echo "Service: $SERVICE_NAME"
echo "VNPay TMN Code: $VNP_TMNCODE"
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
  --set-env-vars VNP_RETURN_URL=com.example.testing5://vnpay-result \
  --set-env-vars VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

# Get service URL
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region $REGION --format 'value(status.url)')

echo ""
echo "✅ Deployment complete!"
echo "📱 Service URL: $SERVICE_URL"
echo ""
echo "⚠️  IMPORTANT: Update IPN URL in VNPay Dashboard"
echo "   1. Go to: https://sandbox.vnpayment.vn/merchantv2/"
echo "   2. Login with: minhvbhe187102@fpt.edu.vn"
echo "   3. Set IPN URL to: $SERVICE_URL/payments/vnpay/ipn"
echo ""
echo "📱 Next: Update Android app with this URL:"
echo "   In app/build.gradle.kts, line 20:"
echo "   buildConfigField(\"String\", \"VNPAY_BACKEND_BASE_URL\", \"\\\"$SERVICE_URL/\\\"\")"
echo ""
echo "🧪 Test the health endpoint:"
echo "   curl $SERVICE_URL/health"

