const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const qs = require('qs');
const admin = require('firebase-admin');

const app = express();
app.use(cors());
app.use(express.json());

if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

const VNP_VERSION = '2.1.0';
const VNP_COMMAND = 'pay';
const VNP_TMN_CODE = process.env.VNP_TMNCODE || '';
const VNP_HASH_SECRET = process.env.VNP_HASHSECRET || '';
const VNP_RETURN_URL = process.env.VNP_RETURN_URL || 'yourapp://vnpay-result';
const VNP_PAY_URL = process.env.VNP_PAY_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';

// Helper function to sort object keys and encode values like VNPay sample implementation
function sortObject(obj) {
  const sorted = {};
  const encodedKeys = Object.keys(obj)
    .map(key => encodeURIComponent(key))
    .sort();

  encodedKeys.forEach(encodedKey => {
    const originalKey = decodeURIComponent(encodedKey);
    sorted[encodedKey] = encodeURIComponent(obj[originalKey]).replace(/%20/g, "+");
  });

  return sorted;
}

// Create VNPay payment URL
function createVnPayUrl(amountVnd, orderId, orderInfo, ipAddr) {
  const createDate = new Date().toISOString()
    .replace(/[-:TZ.]/g, '')
    .slice(0, 14); // yyyyMMddHHmmss format

  const params = {
    vnp_Version: VNP_VERSION,
    vnp_Command: VNP_COMMAND,
    vnp_TmnCode: VNP_TMN_CODE,
    vnp_Amount: amountVnd.toString(),
    vnp_CurrCode: 'VND',
    vnp_TxnRef: orderId,
    vnp_OrderInfo: orderInfo,
    vnp_OrderType: 'other',
    vnp_Locale: 'vn',
    vnp_ReturnUrl: VNP_RETURN_URL,
    vnp_IpAddr: ipAddr,
    vnp_CreateDate: createDate
  };

  // Sort parameters
  const sortedParams = sortObject(params);

  // Create query string for signing (without encoding)
  const signData = qs.stringify(sortedParams, { encode: false });

  // Create HMAC SHA512 hash
  const secureHash = crypto
    .createHmac('sha512', VNP_HASH_SECRET)
    .update(Buffer.from(signData, 'utf-8'))
    .digest('hex');

  // Create query string for URL (with encoding)
  const queryString = qs.stringify(sortedParams, { encode: false });
  const paymentUrl = `${VNP_PAY_URL}?${queryString}&vnp_SecureHash=${secureHash}`;

  return paymentUrl;
}

async function savePendingOrder(orderId, userId, amountVnd, currencyReward, orderDescription) {
  if (!userId) {
    throw new Error('User ID is required to create order');
  }

  const amountVndActual = Math.round(Number(amountVnd || 0) / 100);
  const currencyRewardValue = Number(currencyReward || 0);

  const orderData = {
    userId,
    status: 'pending',
    currencyReward: currencyRewardValue,
    amountVnp: Number(amountVnd || 0),
    amountVnd: amountVndActual,
    orderDescription: orderDescription || null,
    updatedAt: FieldValue.serverTimestamp()
  };

  const orderRef = firestore.collection('vnpay_orders').doc(orderId);
  await orderRef.set(
    {
      ...orderData,
      createdAt: FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  return orderData;
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Create payment endpoint (generic)
app.post('/payments/vnpay/create', async (req, res) => {
  try {
    const { amountVnd, currencyReward, userId, orderDescription } = req.body;

    if (!amountVnd || !currencyReward || !userId) {
      return res.status(400).json({
        error: 'Missing required fields: amountVnd, currencyReward, and userId'
      });
    }

    if (!VNP_TMN_CODE || !VNP_HASH_SECRET) {
      return res.status(500).json({
        error: 'VNPay credentials not configured. Set VNP_TMNCODE and VNP_HASHSECRET environment variables.'
      });
    }

    const orderId = `ORDER_${Date.now()}_${userId || 'ANON'}`;
    const orderInfo = orderDescription || `Top up ${currencyReward} currency`;
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0] || 
                    req.socket.remoteAddress || 
                    '127.0.0.1';

    const paymentUrl = createVnPayUrl(amountVnd, orderId, orderInfo, clientIp);

    await savePendingOrder(orderId, userId, amountVnd, currencyReward, orderInfo);

    res.json({
      orderId,
      amountVnd: amountVnd / 100, // Convert back to actual VND (divide by 100)
      currencyReward,
      paymentUrl
    });
  } catch (error) {
    console.error('Error creating payment:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  }
});

// Specific endpoint for 10k package
app.post('/payments/vnpay/create10k', async (req, res) => {
  try {
    if (!VNP_TMN_CODE || !VNP_HASH_SECRET) {
      return res.status(500).json({
        error: 'VNPay credentials not configured'
      });
    }

    const userId = req.body.userId;

    if (!userId) {
      return res.status(400).json({ error: 'Missing required field: userId' });
    }

    const orderId = `TOPUP10K_${Date.now()}_${userId}`;
    const orderInfo = 'Top up 100 currency';
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0] || 
                    req.socket.remoteAddress || 
                    '127.0.0.1';

    // 10,000 VND = 1,000,000 in VNPay format (multiply by 100)
    const paymentUrl = createVnPayUrl(1000000, orderId, orderInfo, clientIp);

    await savePendingOrder(orderId, userId, 1000000, 100, orderInfo);

    res.json({
      orderId,
      amountVnd: 10000,
      currencyReward: 100,
      paymentUrl
    });
  } catch (error) {
    console.error('Error creating 10k payment:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  }
});

async function handleIpnRequest(req, res) {
  try {
    const vnpParams = { ...req.query };

    const secureHash = vnpParams['vnp_SecureHash'];

    delete vnpParams['vnp_SecureHash'];
    delete vnpParams['vnp_SecureHashType'];

    // Sort and create sign data
    const sortedParams = sortObject(vnpParams);
    const signData = qs.stringify(sortedParams, { encode: false });

    // Verify signature
    const checkSum = crypto
      .createHmac('sha512', VNP_HASH_SECRET)
      .update(Buffer.from(signData, 'utf-8'))
      .digest('hex');

    if (secureHash !== checkSum) {
      console.error('Invalid IPN signature');
      return res.status(200).json({ RspCode: '97', Message: 'Checksum failed' });
    }

    const responseCode = vnpParams['vnp_ResponseCode'];
    const transactionStatus = vnpParams['vnp_TransactionStatus'];
    const orderId = vnpParams['vnp_TxnRef'];
    const amount = vnpParams['vnp_Amount'];
    const transactionNo = vnpParams['vnp_TransactionNo'] || null;
    const payDate = vnpParams['vnp_PayDate'] || null;

    console.log('IPN received:', {
      orderId,
      responseCode,
      transactionStatus,
      amount,
      transactionNo,
      payDate
    });

    const transactionResult = await firestore.runTransaction(async (transaction) => {
      const orderRef = firestore.collection('vnpay_orders').doc(orderId);
      const orderDoc = await transaction.get(orderRef);

      if (!orderDoc.exists) {
        console.warn('Order not found for IPN:', orderId);
        return { status: 'missing' };
      }

      const orderData = orderDoc.data() || {};
      const orderUpdate = {
        vnp_ResponseCode: responseCode,
        vnp_TransactionStatus: transactionStatus,
        vnp_TransactionNo: transactionNo,
        vnp_PayDate: payDate,
        vnp_Amount: amount,
        updatedAt: FieldValue.serverTimestamp()
      };

      if (responseCode === '00' && transactionStatus === '00') {
        if (orderData.status !== 'success') {
          orderUpdate.status = 'success';
          transaction.update(orderRef, orderUpdate);

          if (orderData.userId && orderData.currencyReward) {
            const userRef = firestore.collection('users').doc(orderData.userId);
            transaction.update(userRef, {
              currency: FieldValue.increment(orderData.currencyReward)
            });

            console.log(`Credited ${orderData.currencyReward} currency to user ${orderData.userId}`);
          }

          return { status: 'credited' };
        }

        // Already processed success, just update metadata
        transaction.update(orderRef, orderUpdate);
        return { status: 'already_success' };
      } else {
        orderUpdate.status = 'failed';
        transaction.update(orderRef, orderUpdate);
        console.warn('Transaction failed according to VNPay response', orderId, responseCode, transactionStatus);
        return { status: 'failed' };
      }
    });

    if (transactionResult.status === 'missing') {
      return res.status(200).json({ RspCode: '01', Message: 'Order not found' });
    }

    return res.status(200).json({ RspCode: '00', Message: 'Success' });
  } catch (error) {
    console.error('IPN error:', error);
    res.status(200).json({ RspCode: '99', Message: error.message });
  }
}

app.post('/payments/vnpay/ipn', handleIpnRequest);
app.get('/payments/vnpay/ipn', handleIpnRequest);

// VNPay Return URL handler (for web redirects)
app.get('/payments/vnpay/return', (req, res) => {
  try {
    const vnpParams = req.query;
    const secureHash = vnpParams['vnp_SecureHash'];

    delete vnpParams['vnp_SecureHash'];
    delete vnpParams['vnp_SecureHashType'];

    const sortedParams = sortObject(vnpParams);
    const signData = qs.stringify(sortedParams, { encode: false });

    const checkSum = crypto
      .createHmac('sha512', VNP_HASH_SECRET)
      .update(Buffer.from(signData, 'utf-8'))
      .digest('hex');

    if (secureHash === checkSum) {
      const responseCode = vnpParams['vnp_ResponseCode'];
      if (responseCode === '00') {
        res.send(`
          <html>
            <body>
              <h1>Payment Successful!</h1>
              <p>Order ID: ${vnpParams['vnp_TxnRef']}</p>
              <p>Please return to the app to see your updated balance.</p>
            </body>
          </html>
        `);
      } else {
        res.send(`
          <html>
            <body>
              <h1>Payment Failed</h1>
              <p>Response Code: ${responseCode}</p>
            </body>
          </html>
        `);
      }
    } else {
      res.send('<html><body><h1>Invalid signature</h1></body></html>');
    }
  } catch (error) {
    res.send(`<html><body><h1>Error: ${error.message}</h1></body></html>`);
  }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`VNPay backend server running on port ${PORT}`);
  console.log(`VNPay TMN Code: ${VNP_TMN_CODE ? 'Configured' : 'NOT SET'}`);
  console.log(`VNPay Hash Secret: ${VNP_HASH_SECRET ? 'Configured' : 'NOT SET'}`);
});

