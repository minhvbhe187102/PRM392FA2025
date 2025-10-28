package com.example.testing5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText, passwordEditText, usernameEditText;
    private Button loginButton;
    private TextView switchModeText;
    private CheckBox rememberMeCheckBox;
    private boolean isLoginMode = true;
    
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_REMEMBER_ME = "remember_me";
    
    private FirebaseAuth mAuth;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firebaseService = FirebaseService.getInstance();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        loginButton = findViewById(R.id.loginButton);
        switchModeText = findViewById(R.id.switchModeText);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        
        // Check if user is already logged in
        checkAutoLogin();
        
        // Load saved email if remember me was checked
        loadSavedLoginInfo();
        
        // Set initial state
        updateUI();
        
        // Button listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginMode) {
                    loginUser();
                } else {
                    signupUser();
                }
            }
        });
        
        
        switchModeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            loginButton.setText("LOGIN");
            switchModeText.setText("Don't have an account? Sign up");
            usernameEditText.setVisibility(View.GONE);
        } else {
            loginButton.setText("SIGN UP");
            switchModeText.setText("Already have an account? Login");
            usernameEditText.setVisibility(View.VISIBLE);
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save login information if remember me is checked
                                saveLoginInfo(email, rememberMeCheckBox.isChecked());
                                // Check if user profile exists, create if not
                                checkAndCreateUserProfile(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signupUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Create user profile
                                createUserProfile(user, username);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Registration failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createUserProfile(FirebaseUser firebaseUser, String username) {
        User user = new User(firebaseUser.getUid(), username, firebaseUser.getEmail());
        
        firebaseService.createUserProfile(user, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User profile created successfully");
                    // Create free default skin for new user
                    createDefaultSkinForUser(firebaseUser.getUid());
                    Toast.makeText(LoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMainMenu();
                } else {
                    Log.e(TAG, "Error creating user profile", task.getException());
                    Toast.makeText(LoginActivity.this, "Error creating profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void createDefaultSkinForUser(String userId) {
        // Create a unique default skin for this user (circular)
        SkinManager skinManager = SkinManager.getInstance();
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xFF00FF00); // Green
        canvas.drawCircle(32, 32, 32, paint);
        String base64 = skinManager.bitmapToBase64(bitmap);
        
        String skinId = "default_" + userId; // Unique default skin per user
        Skin defaultSkin = new Skin(skinId, "Default Skin", "Your free starter skin", 0, base64, userId, true, Skin.Rarity.COMMON);
        defaultSkin.setCurrentOwner(userId); // Assign ownership immediately
        defaultSkin.setForSale(false); // Not for sale - personal skin
        
        FirebaseService.getInstance().createSkin(defaultSkin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Default skin created for user " + userId);
                } else {
                    Log.e(TAG, "Error creating default skin", task.getException());
                }
            }
        });
    }

    private void checkAndCreateUserProfile(FirebaseUser firebaseUser) {
        firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // User profile exists, navigate to main menu
                        navigateToMainMenu();
                    } else {
                        // User profile doesn't exist, create one
                        User user = new User(firebaseUser.getUid(), 
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Player", 
                                firebaseUser.getEmail());
                        createUserProfile(firebaseUser, user.getUsername());
                    }
                } else {
                    Log.e(TAG, "Error checking user profile", task.getException());
                    Toast.makeText(LoginActivity.this, "Error checking profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void navigateToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    private void checkAutoLogin() {
        // Check if user is already logged in with Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to main menu
            navigateToMainMenu();
        }
    }
    
    private void loadSavedLoginInfo() {
        // Load saved email if remember me was checked
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            if (!savedEmail.isEmpty()) {
                emailEditText.setText(savedEmail);
                rememberMeCheckBox.setChecked(true);
            }
        }
    }
    
    private void saveLoginInfo(String email, boolean rememberMe) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (rememberMe) {
            editor.putString(KEY_EMAIL, email);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            editor.remove(KEY_EMAIL);
            editor.putBoolean(KEY_REMEMBER_ME, false);
        }
        editor.apply();
    }
    
    private void clearSavedLoginInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.apply();
    }
}
