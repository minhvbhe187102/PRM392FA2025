package com.example.testing5;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Handles VNPay return URL deep link: com.example.testing5://vnpay-result
 * This activity receives the payment result from VNPay and refreshes the user's balance.
 */
public class VnPayReturnActivity extends AppCompatActivity {

    private static final String TAG = "VnPayReturnActivity";

    private FirebaseService firebaseService;
    private TextView statusText;
    private String orderId;
    private String responseCode;
    private Handler refreshHandler;
    private int refreshAttempts = 0;
    private static final int MAX_REFRESH_ATTEMPTS = 3;
    private static final long REFRESH_DELAY_MS = 2500L;
    private int baselineCurrency = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_return);

        statusText = findViewById(R.id.textStatus);
        firebaseService = FirebaseService.getInstance();
        refreshHandler = new Handler(Looper.getMainLooper());

        // Handle the deep link intent
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            Log.d(TAG, "Received deep link: " + data.toString());
            orderId = data.getQueryParameter("vnp_TxnRef");
            responseCode = data.getQueryParameter("vnp_ResponseCode");

            if ("00".equals(responseCode)) {
                statusText.setText(getString(R.string.vnpay_return_success_fetching, orderId));
                refreshUserBalance();
            } else {
                statusText.setText(getString(R.string.vnpay_return_failed,
                        orderId != null ? orderId : getString(R.string.vnpay_return_unknown_order),
                        responseCode != null ? responseCode : "--"));
            }
        } else {
            statusText.setText(R.string.vnpay_return_no_data);
        }

        // Auto-close after 3 seconds and return to main menu
        findViewById(R.id.buttonClose).setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainMenuActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
    }

    private void refreshUserBalance() {
        if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
            statusText.setText(R.string.vnpay_return_refresh_failed);
            return;
        }

        refreshAttempts++;

        FirebaseUser firebaseUser = firebaseService.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, R.string.vnpay_return_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Toast.makeText(VnPayReturnActivity.this,
                            R.string.vnpay_return_refresh_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = firebaseService.documentToUser(task.getResult());
                if (user == null) {
                    Toast.makeText(VnPayReturnActivity.this,
                            R.string.vnpay_return_refresh_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (baselineCurrency < 0) {
                    baselineCurrency = user.getCurrency();
                }

                if (user.getCurrency() > baselineCurrency) {
                    statusText.setText(getString(R.string.vnpay_return_success,
                            orderId != null ? orderId : getString(R.string.vnpay_return_unknown_order),
                            user.getCurrency()));
                    Toast.makeText(VnPayReturnActivity.this,
                            R.string.vnpay_return_balance_updated,
                            Toast.LENGTH_SHORT).show();
                } else if (refreshAttempts < MAX_REFRESH_ATTEMPTS) {
                    statusText.setText(getString(R.string.vnpay_return_pending,
                            orderId != null ? orderId : getString(R.string.vnpay_return_unknown_order)));
                    refreshHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshUserBalance();
                        }
                    }, REFRESH_DELAY_MS);
                } else {
                    statusText.setText(getString(R.string.vnpay_return_refresh_failed));
                }
            }
        });
    }
}

