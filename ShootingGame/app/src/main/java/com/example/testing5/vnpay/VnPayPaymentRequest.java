package com.example.testing5.vnpay;

import com.google.gson.annotations.SerializedName;

public class VnPayPaymentRequest {

    @SerializedName("amountVnd")
    private final long amountVnd;

    @SerializedName("currencyReward")
    private final long currencyReward;

    @SerializedName("userId")
    private final String userId;

    @SerializedName("orderDescription")
    private final String orderDescription;

    public VnPayPaymentRequest(long amountVnd, long currencyReward, String userId, String orderDescription) {
        this.amountVnd = amountVnd;
        this.currencyReward = currencyReward;
        this.userId = userId;
        this.orderDescription = orderDescription;
    }

    public long getAmountVnd() {
        return amountVnd;
    }

    public long getCurrencyReward() {
        return currencyReward;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderDescription() {
        return orderDescription;
    }
}
