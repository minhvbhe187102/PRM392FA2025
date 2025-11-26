# VNPay Backend Deployment Guide

## Step-by-Step Instructions

### Step 1: Enable Required APIs

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/library?project=shottinggamepayment)
2. Enable these APIs:
   - Cloud Run API
   - Cloud Build API
   - Secret Manager API (optional, for secure credential storage)

Or use gcloud CLI:
```bash
gcloud config set project shottinggamepayment
gcloud services enable run.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com
```

### Step 2: Get VNPay Sandbox Credentials

1. Visit: https://sandbox.vnpayment.vn/devreg/
2. Register and receive:
   - `vnp_TmnCode` (e.g., `DEMOV210`)
   - `vnp_HashSecret` (a long secret string)

**Save these credentials - you'll need them in Step 4!**

### Step 3: Prepare Backend Code

The backend code is already in the `backend/` folder. Make sure you have:
- `backend/package.json`
- `backend/index.js`
- `backend/Dockerfile`

### Step 4: Deploy to Cloud Run

#### Install Google Cloud CLI (if not installed)

Download from: https://cloud.google.com/sdk/docs/install

#### Authenticate

```bash
gcloud auth login
gcloud config set project shottinggamepayment
```

#### Build and Deploy

Navigate to the `backend` folder and run:

```bash
cd backend

# Build and submit to Container Registry
gcloud builds submit --tag gcr.io/shottinggamepayment/vnpay-backend

# Deploy to Cloud Run
gcloud run deploy vnpay-backend \
  --image gcr.io/shottinggamepayment/vnpay-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars VNP_TMNCODE=YOUR_TMN_CODE_HERE \
  --set-env-vars VNP_HASHSECRET=YOUR_HASH_SECRET_HERE \
  --set-env-vars VNP_RETURN_URL=yourapp://vnpay-result \
  --set-env-vars VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

**Replace `YOUR_TMN_CODE_HERE` and `YOUR_HASH_SECRET_HERE` with your actual VNPay credentials!**

### Step 5: Get Your Cloud Run URL

After deployment completes, you'll see output like:
```
Service [vnpay-backend] revision [vnpay-backend-00001-abc] has been deployed and is serving 100 percent of traffic.
Service URL: https://vnpay-backend-xxxxx-uc.a.run.app
```

**Copy this URL!**

### Step 6: Update Android App

1. Open `app/build.gradle.kts`
2. Find line 20:
   ```kotlin
   buildConfigField("String", "VNPAY_BACKEND_BASE_URL", "\"https://your-backend.example.com/\"")
   ```
3. Replace with your Cloud Run URL (make sure it ends with `/`):
   ```kotlin
   buildConfigField("String", "VNPAY_BACKEND_BASE_URL", "\"https://vnpay-backend-xxxxx-uc.a.run.app/\"")
   ```
4. Sync Gradle
5. Rebuild the app

### Step 7: Test the Integration

1. Run your Android app
2. Go to "BUY CURRENCY" menu
3. Click "10.000₫ → Get 100 coins"
4. You should see a loading indicator, then VNPay payment page should open

### Step 8: Configure IPN URL (Important!)

VNPay needs to notify your backend when payment completes:

1. Go to your VNPay merchant dashboard
2. Set IPN URL to: `https://your-cloud-run-url/payments/vnpay/ipn`
3. This allows your backend to automatically credit currency to users

### Troubleshooting

**Error: "VNPay credentials not configured"**
- Make sure you set `VNP_TMNCODE` and `VNP_HASHSECRET` environment variables during deployment

**Error: "Unable to generate VNPay link"**
- Check Cloud Run logs: `gcloud run services logs read vnpay-backend --region us-central1`
- Verify your VNPay credentials are correct

**Payment URL doesn't open**
- Check that Custom Tabs are available on your device
- Verify the payment URL in Cloud Run logs

### Next Steps

1. **Implement Firestore update in IPN handler** - Currently the IPN endpoint just logs. You need to add code to update user currency in Firestore when payment succeeds.

2. **Add authentication** - Consider adding API key or Firebase Auth token verification to secure your endpoints.

3. **Handle deep link** - Implement `yourapp://vnpay-result` deep link handler in Android to refresh balance after payment.

4. **Move to production** - When ready, switch from sandbox to production VNPay URLs and credentials.

