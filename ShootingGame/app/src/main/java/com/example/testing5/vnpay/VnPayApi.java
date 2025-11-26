package com.example.testing5.vnpay;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface VnPayApi {

    @POST("payments/vnpay/create")
    Call<VnPayPaymentResponse> createPayment(@Body VnPayPaymentRequest request);
}
