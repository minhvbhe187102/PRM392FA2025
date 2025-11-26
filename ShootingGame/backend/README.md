# VNPay Backend for Cloud Run

This is a Node.js Express backend service for handling VNPay payment integration.

## Setup Instructions

### 1. Get VNPay Sandbox Credentials

1. Go to https://sandbox.vnpayment.vn/devreg/
2. Register and get your:
   - `vnp_TmnCode` (Terminal Code)
   - `vnp_HashSecret` (Secret Key)

### 2. Deploy to Cloud Run

#### Option A: Using gcloud CLI

```bash
# Set your project
gcloud config set project shottinggamepayment

# Enable required APIs
gcloud services enable run.googleapis.com cloudbuild.googleapis.com

# Build and deploy
gcloud builds submit --tag gcr.io/shottinggamepayment/vnpay-backend
gcloud run deploy vnpay-backend \
  --image gcr.io/shottinggamepayment/vnpay-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars VNP_TMNCODE=YOUR_TMN_CODE \
  --set-env-vars VNP_HASHSECRET=YOUR_HASH_SECRET \
  --set-env-vars VNP_RETURN_URL=yourapp://vnpay-result \
  --set-env-vars VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

#### Option B: Using Secret Manager (Recommended)

```bash
# Create secrets
echo -n "YOUR_TMN_CODE" | gcloud secrets create vnp-tmncode --data-file=-
echo -n "YOUR_HASH_SECRET" | gcloud secrets create vnp-hashsecret --data-file=-

# Deploy with secrets
gcloud run deploy vnpay-backend \
  --image gcr.io/shottinggamepayment/vnpay-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-secrets VNP_TMNCODE=vnp-tmncode:latest,VNP_HASHSECRET=vnp-hashsecret:latest \
  --set-env-vars VNP_RETURN_URL=yourapp://vnpay-result
```

### 3. Get Your Cloud Run URL

After deployment, you'll get a URL like:
```
https://vnpay-backend-xxxxx-uc.a.run.app
```

Use this URL in your Android app's `BuildConfig.VNPAY_BACKEND_BASE_URL`.

## API Endpoints

- `POST /payments/vnpay/create` - Create payment URL (generic)
- `POST /payments/vnpay/create10k` - Create 10k VND payment URL
- `POST /payments/vnpay/ipn` - IPN callback from VNPay
- `GET /payments/vnpay/return` - Return URL handler
- `GET /health` - Health check

## Testing

```bash
# Test health endpoint
curl https://your-cloud-run-url/health

# Test 10k payment creation
curl -X POST https://your-cloud-run-url/payments/vnpay/create10k \
  -H "Content-Type: application/json" \
  -d '{"userId":"test123"}'
```

