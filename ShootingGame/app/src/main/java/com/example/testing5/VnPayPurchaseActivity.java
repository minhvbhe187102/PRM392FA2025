package com.example.testing5;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.testing5.BuildConfig;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

import com.example.testing5.vnpay.VnPayPaymentRequest;
import com.example.testing5.vnpay.VnPayPaymentResponse;
import com.example.testing5.vnpay.VnPayService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Screen that hosts the VNPay purchase entry point. The actual VNPay integration requires
 * generating a payment URL on a secure backend and redirecting the user to VNPay's cashier
 * page. For now we surface the packages and provide a stubbed click handler that you can replace
 * once the backend is ready.
 */
public class VnPayPurchaseActivity extends AppCompatActivity {

    private FirebaseService firebaseService;
    private User currentUser;

    private TextView currencyTextView;
    private TextView instructionsTextView;
    private ProgressBar progressBar;
    private Button buttonTopUp10k;
    private Button buttonTopUp50k;
    private Button buttonTopUp100k;
    private Button buttonTopUp200k;

    private VnPayService vnPayService;
    private Call<VnPayPaymentResponse> pendingPaymentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_purchase);

        firebaseService = FirebaseService.getInstance();
        vnPayService = VnPayService.getInstance();

        currencyTextView = findViewById(R.id.textCurrentCurrency);
        instructionsTextView = findViewById(R.id.textInstructions);
        progressBar = findViewById(R.id.progressGenerating);
        buttonTopUp10k = findViewById(R.id.buttonTopUp10k);
        buttonTopUp50k = findViewById(R.id.buttonTopUp50k);
        buttonTopUp100k = findViewById(R.id.buttonTopUp100k);
        buttonTopUp200k = findViewById(R.id.buttonTopUp200k);

        buttonTopUp10k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVnPayCheckout(1000000L, 100L);
            }
        });

        buttonTopUp50k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVnPayCheckout(50000L, 500L);
            }
        });

        buttonTopUp100k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVnPayCheckout(100000L, 1200L);
            }
        });

        buttonTopUp200k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVnPayCheckout(200000L, 2600L);
            }
        });

        findViewById(R.id.buttonClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingPaymentCall != null) {
            pendingPaymentCall.cancel();
        }
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = firebaseService.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    currentUser = firebaseService.documentToUser(task.getResult());
                    if (currentUser != null) {
                        updateCurrencyDisplay();
                    }
                } else {
                    Toast.makeText(VnPayPurchaseActivity.this,
                            "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateCurrencyDisplay() {
        if (currentUser == null) {
            currencyTextView.setText(getString(R.string.vnpay_currency_placeholder));
            return;
        }

        String currencyText = getString(R.string.vnpay_currency_format,
                currentUser.getCurrency());
        currencyTextView.setText(currencyText);
    }

    private void launchVnPayCheckout(long amountVnd, long currencyReward) {
        if (BuildConfig.VNPAY_BACKEND_BASE_URL.contains("your-backend")) {
            Toast.makeText(this, R.string.vnpay_error_backend_url, Toast.LENGTH_LONG).show();
            return;
        }

        setLoadingState(true);

        String userId = currentUser != null ? currentUser.getUserId() : null;
        if (userId == null || userId.isEmpty()) {
            setLoadingState(false);
            Toast.makeText(this, R.string.vnpay_error_missing_user, Toast.LENGTH_LONG).show();
            return;
        }
        String orderDescription = String.format(Locale.US,
                "Top up %,d coins via VNPay", currencyReward);

        VnPayPaymentRequest request = new VnPayPaymentRequest(amountVnd, currencyReward, userId, orderDescription);
        pendingPaymentCall = vnPayService.createPayment(request);
        pendingPaymentCall.enqueue(new Callback<VnPayPaymentResponse>() {
            @Override
            public void onResponse(@NonNull Call<VnPayPaymentResponse> call,
                                   @NonNull Response<VnPayPaymentResponse> response) {
                setLoadingState(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(VnPayPurchaseActivity.this,
                            getString(R.string.vnpay_error_generating_link, response.code()),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                VnPayPaymentResponse body = response.body();
                if (body == null || !body.hasValidUrl()) {
                    Toast.makeText(VnPayPurchaseActivity.this,
                            R.string.vnpay_error_invalid_response,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                openVnPayCheckout(body.getPaymentUrl());

                if (!TextUtils.isEmpty(body.getOrderId())) {
                    instructionsTextView.setText(getString(R.string.vnpay_after_payment_hint_with_order, body.getOrderId()));
                } else {
                    instructionsTextView.setText(getString(R.string.vnpay_after_payment_hint));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VnPayPaymentResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                setLoadingState(false);
                Toast.makeText(VnPayPurchaseActivity.this,
                        getString(R.string.vnpay_error_network, t.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonTopUp10k.setEnabled(!loading);
        buttonTopUp50k.setEnabled(!loading);
        buttonTopUp100k.setEnabled(!loading);
        buttonTopUp200k.setEnabled(!loading);

        if (loading) {
            instructionsTextView.setText(R.string.vnpay_generating_payment_link);
        } else if (TextUtils.equals(instructionsTextView.getText(),
                getString(R.string.vnpay_generating_payment_link))) {
            instructionsTextView.setText(R.string.vnpay_instruction_text);
        }
    }

    private void openVnPayCheckout(String paymentUrl) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(this, Uri.parse(paymentUrl));
    }
}
