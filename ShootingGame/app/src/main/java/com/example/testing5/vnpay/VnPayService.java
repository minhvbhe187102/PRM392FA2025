package com.example.testing5.vnpay;

import androidx.annotation.VisibleForTesting;

import com.example.testing5.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VnPayService {

    private static volatile VnPayService instance;

    private final VnPayApi api;

    private VnPayService(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(VnPayApi.class);
    }

    public static VnPayService getInstance() {
        if (instance == null) {
            synchronized (VnPayService.class) {
                if (instance == null) {
                    instance = new VnPayService(BuildConfig.VNPAY_BACKEND_BASE_URL);
                }
            }
        }
        return instance;
    }

    @VisibleForTesting
    static void overrideInstance(VnPayService customInstance) {
        instance = customInstance;
    }

    public Call<VnPayPaymentResponse> createPayment(VnPayPaymentRequest request) {
        return api.createPayment(request);
    }
}
