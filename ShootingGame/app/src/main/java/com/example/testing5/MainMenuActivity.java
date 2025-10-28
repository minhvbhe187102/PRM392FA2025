package com.example.testing5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainMenuActivity extends AppCompatActivity {
    private static final String TAG = "MainMenuActivity";
    private FirebaseService firebaseService;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_layout);

        // Initialize Firebase service
        firebaseService = FirebaseService.getInstance();
        
        // Check if user is logged in
        if (!firebaseService.isUserLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get button references
        Button startButton = findViewById(R.id.startButton);
        Button quitButton = findViewById(R.id.quitButton);
        Button inventoryButton = findViewById(R.id.inventoryButton);
        Button pullButton = findViewById(R.id.pullButton);
        Button shopButton = findViewById(R.id.shopButton);
        Button tradeButton = findViewById(R.id.tradeButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        TextView userInfoText = findViewById(R.id.userInfoText);

        // Load user data from Firebase
        loadUserData();

        // Start button - navigate to game
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, shotting.class);
                startActivity(intent);
            }
        });

        // Inventory button - navigate to inventory
        inventoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, InventoryActivity.class);
                startActivity(intent);
            }
        });

        // Pull button - navigate to pull activity
        pullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, PullActivity.class);
                startActivity(intent);
            }
        });

        shopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, ShopActivity.class);
                startActivity(intent);
            }
        });

        // Trade button - navigate to trading room
        tradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, TradingRoomActivity.class);
                startActivity(intent);
            }
        });

        // Quit button - logout and exit
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear saved login information
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("saved_email");
                editor.putBoolean("remember_me", false);
                editor.apply();
                
                firebaseService.signOut();
                Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Settings button - placeholder for future settings
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainMenuActivity.this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data each time we return to the menu to get updated currency/scores
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = firebaseService.getCurrentUser();
        if (firebaseUser != null) {
            firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        currentUser = firebaseService.documentToUser(task.getResult());
                        if (currentUser != null) {
                            updateUserInfo();
                        }
                    } else {
                        Log.e(TAG, "Error loading user data", task.getException());
                    }
                }
            });
        }
    }

    private void updateUserInfo() {
        TextView userInfoText = findViewById(R.id.userInfoText);
        if (currentUser != null && userInfoText != null) {
            userInfoText.setText("Welcome, " + currentUser.getUsername() + 
                    "\nCurrency: " + currentUser.getCurrency() + 
                    "\nHigh Score: " + currentUser.getHighScore());
        }
    }
    
}
