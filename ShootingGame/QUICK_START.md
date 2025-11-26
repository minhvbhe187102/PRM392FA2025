# Quick Start: Deploy VNPay Backend

## Prerequisites

1. ✅ Google Cloud project created: `shottinggamepayment`
2. ✅ Firebase project created: `shottinggamepayment`
3. ⬜ Google Cloud CLI installed
4. ⬜ VNPay sandbox credentials obtained

## Step 1: Install Google Cloud CLI

Download and install from: https://cloud.google.com/sdk/docs/install

After installation, verify:
```bash
gcloud --version
```

## Step 2: Get VNPay Sandbox Credentials

1. Go to: https://sandbox.vnpayment.vn/devreg/
2. When registering, provide these URLs:
   - **Return URL:** `com.example.testing5://vnpay-result` (deep link for mobile app)
   - **IPN URL:** `https://vnpay-backend-temp.run.app/payments/vnpay/ipn` (temporary - update after deployment)
3. After registration, you'll receive:
   - **vnp_TmnCode** (e.g., `DEMOV210`)
   - **vnp_HashSecret** (long secret string)

**📖 See `VNPAY_REGISTRATION_GUIDE.md` for detailed URL configuration instructions.**

## Step 3: Authenticate with Google Cloud

```bash
gcloud auth login
gcloud config set project shottinggamepayment
```

## Step 4: Enable Required APIs

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com
```

## Step 5: Deploy Backend

### Option A: Using the deployment script (Easiest)

```bash
cd backend
chmod +x deploy.sh
./deploy.sh YOUR_TMN_CODE YOUR_HASH_SECRET
```

### Option B: Manual deployment

```bash
cd backend

# Build Docker image
gcloud builds submit --tag gcr.io/shottinggamepayment/vnpay-backend

# Deploy to Cloud Run
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

## Step 6: Get Your Service URL

After deployment, you'll see:
```
Service URL: https://vnpay-backend-xxxxx-uc.a.run.app
```

**Copy this URL!**

## Step 7: Update Android App

1. Open `app/build.gradle.kts`
2. Find line 20 and replace with your Cloud Run URL:
   ```kotlin
   buildConfigField("String", "VNPAY_BACKEND_BASE_URL", "\"https://vnpay-backend-xxxxx-uc.a.run.app/\"")
   ```
3. Sync Gradle in Android Studio
4. Rebuild the app

## Step 8: Test

1. Run the Android app
2. Navigate to "BUY CURRENCY"
3. Click "10.000₫ → Get 100 coins"
4. VNPay payment page should open

## Troubleshooting

**Can't deploy?**
- Make sure billing is enabled: https://console.cloud.google.com/billing?project=shottinggamepayment
- Check you're authenticated: `gcloud auth list`

**Backend returns error?**
- Check logs: `gcloud run services logs read vnpay-backend --region us-central1`
- Verify VNPay credentials are correct

**Payment URL not opening?**
- Check Cloud Run logs for the generated URL
- Verify Custom Tabs are available on your device

## Next Steps

See `DEPLOYMENT_GUIDE.md` for detailed instructions and IPN configuration.

