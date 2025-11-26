# 🚀 Deploy VNPay Backend NOW

You have your VNPay credentials! Let's deploy immediately.

## Your VNPay Credentials (from email)

- **vnp_TmnCode:** `6ZLM2DWT`
- **vnp_HashSecret:** `3W7PP6S7VEJSTIG8YVUS9B24FLZVWVDN`
- **Payment URL:** `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`

## Quick Deployment (3 Steps)

### Step 1: Install Google Cloud CLI (if not done)

Download: https://cloud.google.com/sdk/docs/install

Then authenticate:
```bash
gcloud auth login
gcloud config set project shottinggamepayment
```

### Step 2: Enable APIs

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com
```

### Step 3: Deploy

```bash
cd backend
chmod +x deploy-with-credentials.sh
./deploy-with-credentials.sh
```

**That's it!** The script will:
- Build your Docker image
- Deploy to Cloud Run
- Show you the service URL

## After Deployment

### 1. Get Your Service URL

The script will output something like:
```
Service URL: https://vnpay-backend-xxxxx-uc.a.run.app
```

**Copy this URL!**

### 2. Update IPN URL in VNPay Dashboard

1. Go to: https://sandbox.vnpayment.vn/merchantv2/
2. Login with:
   - Email: `minhvbhe187102@fpt.edu.vn`
   - Password: (your registration password)
3. Find IPN URL settings
4. Set to: `https://vnpay-backend-xxxxx-uc.a.run.app/payments/vnpay/ipn`
   (Replace with your actual Cloud Run URL)

### 3. Update Android App

1. Open `app/build.gradle.kts`
2. Find line 20:
   ```kotlin
   buildConfigField("String", "VNPAY_BACKEND_BASE_URL", "\"https://your-backend.example.com/\"")
   ```
3. Replace with your Cloud Run URL (must end with `/`):
   ```kotlin
   buildConfigField("String", "VNPAY_BACKEND_BASE_URL", "\"https://vnpay-backend-xxxxx-uc.a.run.app/\"")
   ```
4. Sync Gradle in Android Studio
5. Rebuild the app

## Test Payment

1. Run your Android app
2. Go to "BUY CURRENCY"
3. Click "10.000₫ → Get 100 coins"
4. VNPay payment page should open
5. Use test card:
   - **Card:** 9704198526191432198
   - **Name:** NGUYEN VAN A
   - **Expiry:** 07/15
   - **OTP:** 123456

## Troubleshooting

**Deployment fails?**
- Check billing is enabled: https://console.cloud.google.com/billing?project=shottinggamepayment
- Verify you're authenticated: `gcloud auth list`

**Can't access Cloud Run?**
- Check logs: `gcloud run services logs read vnpay-backend --region us-central1`

**Payment URL not opening?**
- Verify the service URL is correct in `build.gradle.kts`
- Check Cloud Run logs for errors

## Manual Deployment (Alternative)

If the script doesn't work, deploy manually:

```bash
cd backend

# Build
gcloud builds submit --tag gcr.io/shottinggamepayment/vnpay-backend

# Deploy
gcloud run deploy vnpay-backend \
  --image gcr.io/shottinggamepayment/vnpay-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars VNP_TMNCODE=6ZLM2DWT \
  --set-env-vars VNP_HASHSECRET=3W7PP6S7VEJSTIG8YVUS9B24FLZVWVDN \
  --set-env-vars VNP_RETURN_URL=com.example.testing5://vnpay-result \
  --set-env-vars VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

## Next Steps

After successful deployment and testing:
1. ✅ Backend is live on Cloud Run
2. ✅ Android app can create payment URLs
3. ✅ VNPay can send IPN notifications
4. ⬜ Implement Firestore currency update in IPN handler
5. ⬜ Add deep link result parsing in `VnPayReturnActivity`

