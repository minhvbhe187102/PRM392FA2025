package com.example.testing5.vnpay; 

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class VnPayPaymentResponse {

    @SerializedName("paymentUrl")
    private String paymentUrl;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("message")
    private String message;

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasValidUrl() {
        return !TextUtils.isEmpty(paymentUrl);
    }
}
