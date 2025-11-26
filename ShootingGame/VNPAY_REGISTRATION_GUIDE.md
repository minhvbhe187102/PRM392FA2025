# VNPay Sandbox Registration - URL Configuration Guide

## What URLs VNPay Needs

When registering at https://sandbox.vnpayment.vn/devreg/, VNPay will ask for:

### 1. **Return URL** (vnp_ReturnUrl)
**What it is:** Where users are redirected after completing payment  
**For mobile apps:** Use a deep link (custom URL scheme)

**Enter this:**
```
com.example.testing5://vnpay-result
```

Or if they require HTTP/HTTPS format:
```
https://your-app-domain.com/vnpay-return
```

**Note:** For Android apps, the deep link format `com.example.testing5://vnpay-result` is preferred. This will redirect users back to your app after payment.

### 2. **IPN URL** (Instant Payment Notification)
**What it is:** Server endpoint where VNPay sends payment status updates  
**Format:** Must be HTTPS

**Temporary placeholder (use this initially):**
```
https://vnpay-backend-temp.run.app/payments/vnpay/ipn
```

**After you deploy Cloud Run, update to:**
```
https://vnpay-backend-xxxxx-uc.a.run.app/payments/vnpay/ipn
```

## Step-by-Step Registration

### Step 1: Initial Registration

1. Go to: https://sandbox.vnpayment.vn/devreg/
2. Fill in the form with:
   - **Website/App Name:** Shooting Game Payment
   - **Return URL:** `com.example.testing5://vnpay-result`
   - **IPN URL:** `https://vnpay-backend-temp.run.app/payments/vnpay/ipn` (temporary)
   - **Business Information:** Your details

3. Submit and wait for email with:
   - `vnp_TmnCode`
   - `vnp_HashSecret`

### Step 2: Deploy Your Backend

After you get credentials, deploy to Cloud Run (see `QUICK_START.md`).

You'll get a URL like:
```
https://vnpay-backend-xxxxx-uc.a.run.app
```

### Step 3: Update IPN URL in VNPay Dashboard

1. Log into VNPay merchant dashboard
2. Go to Settings/Configuration
3. Update IPN URL to:
   ```
   https://vnpay-backend-xxxxx-uc.a.run.app/payments/vnpay/ipn
   ```
   (Replace with your actual Cloud Run URL)

## Alternative: Use HTTP Return URL

If VNPay doesn't accept deep links for Return URL, you can:

1. **Option A:** Use your Cloud Run return endpoint:
   ```
   https://vnpay-backend-xxxxx-uc.a.run.app/payments/vnpay/return
   ```
   This will show a web page, then users manually return to app.

2. **Option B:** Create a simple redirect page that opens your app:
   - Host a page that redirects to `com.example.testing5://vnpay-result`
   - Use that page URL as Return URL

## Important Notes

- **Return URL** can be updated later in VNPay dashboard
- **IPN URL** is critical - make sure it's accessible via HTTPS
- Deep links work best for mobile apps
- You can test with placeholder URLs first, then update after deployment

## Testing Deep Link

After setting up, test the deep link on Android:

```bash
adb shell am start -a android.intent.action.VIEW -d "com.example.testing5://vnpay-result"
```

This should open your app (you'll need to implement the deep link handler first).

