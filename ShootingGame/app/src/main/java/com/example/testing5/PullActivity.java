package com.example.testing5;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Random;

public class PullActivity extends AppCompatActivity {
    private static final String TAG = "PullActivity";
    
    private Button pullButton;
    private Button backButton;
    private FirebaseService firebaseService;
    private FirebaseAuth firebaseAuth;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull);
        
        initializeViews();
        initializeFirebase();
        loadUserData();
    }
    
    private void initializeViews() {
        pullButton = findViewById(R.id.pullButton);
        backButton = findViewById(R.id.backButton);
        
        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        pullButton.setOnClickListener(v -> performPull());
    }
    
    private void initializeFirebase() {
        firebaseService = FirebaseService.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    private void loadUserData() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load user profile
        firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                    } else {
                        Toast.makeText(PullActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Log.e(TAG, "Error loading user profile", task.getException());
                    Toast.makeText(PullActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
    
    private void performPull() {
        if (currentUser == null) {
            Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For testing, cost is set to 0
        int pullCost = 0; // Set to 0 for testing as requested
        
        if (currentUser.getCurrency() < pullCost) {
            Toast.makeText(this, "Not enough currency! Need " + pullCost + " coins.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button during pull
        pullButton.setEnabled(false);
        pullButton.setText("PULLING...");
        
        // Generate random skin
        Skin randomSkin = generateRandomSkin();
        
        // Add skin to user's inventory
        firebaseService.createSkin(randomSkin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Update user's currency and unlocked skins
                    currentUser.setCurrency(currentUser.getCurrency() - pullCost);
                    currentUser.getUnlockedSkins().add(randomSkin.getSkinId());
                    
                    // Update user profile
                    firebaseService.updateUserProfile(currentUser, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> updateTask) {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(PullActivity.this, 
                                    "Pulled: " + randomSkin.getName() + "!", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Error updating user profile", updateTask.getException());
                                Toast.makeText(PullActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                            }
                            
                            // Re-enable button
                            pullButton.setEnabled(true);
                            pullButton.setText("PULL!!!");
                        }
                    });
                } else {
                    Log.e(TAG, "Error creating skin", task.getException());
                    Toast.makeText(PullActivity.this, "Error creating skin", Toast.LENGTH_SHORT).show();
                    
                    // Re-enable button
                    pullButton.setEnabled(true);
                    pullButton.setText("PULL!!!");
                }
            }
        });
    }
    
    private Skin generateRandomSkin() {
        Random random = new Random();
        
        // Generate random background color
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        int backgroundColor = 0xFF000000 | (red << 16) | (green << 8) | blue;
        
        // Determine rarity
        Skin.Rarity rarity = RarityConfig.determineRarity(random);
        
        // Create bitmap with rarity-specific visual elements
        android.graphics.Bitmap bitmap = createSkinBitmap(backgroundColor, rarity, random);
        
        // Convert to base64
        String base64 = SkinManager.getInstance().bitmapToBase64(bitmap);
        
        // Generate unique skin ID
        String skinId = SkinManager.getInstance().generateSkinId();
        
        // Create skin name based on color and rarity
        String colorName = getColorName(red, green, blue);
        String skinName = colorName + " " + rarity.getDisplayName() + " Skin";
        String description = "A randomly generated " + colorName.toLowerCase() + " " + rarity.getDisplayName().toLowerCase() + " skin";
        
        // Create skin object
        Skin skin = new Skin(skinId, skinName, description, 0, base64, currentUser.getUserId(), true, rarity);
        skin.setCurrentOwner(currentUser.getUserId());
        skin.setForSale(false); // Not for sale - personal skin
        
        return skin;
    }
    
    private android.graphics.Bitmap createSkinBitmap(int backgroundColor, Skin.Rarity rarity, Random random) {
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        
        // Draw background circle
        paint.setColor(backgroundColor);
        canvas.drawCircle(32, 32, 32, paint);
        
        // Add rarity-specific visual elements
        switch (rarity) {
            case COMMON:
                // Common skins only have background color (no additional elements)
                break;
                
            case UNCOMMON:
                // Add smiley face (two dots for eyes and curved mouth) - much bigger
                paint.setColor(generateContrastingColor(backgroundColor, random));
                paint.setStyle(android.graphics.Paint.Style.FILL);
                
                // Draw eyes (two dots) - bigger and better positioned
                float eyeY = 18; // Position eyes higher
                float leftEyeX = 22; // Left eye
                float rightEyeX = 42; // Right eye
                float eyeRadius = 4; // Bigger eye size
                canvas.drawCircle(leftEyeX, eyeY, eyeRadius, paint);
                canvas.drawCircle(rightEyeX, eyeY, eyeRadius, paint);
                
                // Draw smile (curved line) - much bigger
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(6); // Thicker line
                android.graphics.RectF smileRect = new android.graphics.RectF(16, 28, 48, 44); // Bigger smile
                canvas.drawArc(smileRect, 0, 180, false, paint);
                break;
                
            case RARE:
                // Add star shape (nearly fills the circle)
                paint.setColor(generateContrastingColor(backgroundColor, random));
                drawStar(canvas, paint, 32, 32, 28, 14, 5); // Nearly fills the 32-radius circle
                break;
        }
        
        return bitmap;
    }
    
    private int generateContrastingColor(int backgroundColor, Random random) {
        // Extract RGB components
        int r = (backgroundColor >> 16) & 0xFF;
        int g = (backgroundColor >> 8) & 0xFF;
        int b = backgroundColor & 0xFF;
        
        // Generate contrasting color (opposite on color wheel or random)
        int contrastR = 255 - r;
        int contrastG = 255 - g;
        int contrastB = 255 - b;
        
        // Add some randomness to make it more interesting
        contrastR = Math.max(0, Math.min(255, contrastR + random.nextInt(100) - 50));
        contrastG = Math.max(0, Math.min(255, contrastG + random.nextInt(100) - 50));
        contrastB = Math.max(0, Math.min(255, contrastB + random.nextInt(100) - 50));
        
        return 0xFF000000 | (contrastR << 16) | (contrastG << 8) | contrastB;
    }
    
    private void drawStar(android.graphics.Canvas canvas, android.graphics.Paint paint, 
                         float centerX, float centerY, float outerRadius, float innerRadius, int numPoints) {
        android.graphics.Path path = new android.graphics.Path();
        
        float angle = (float) (Math.PI / numPoints);
        
        for (int i = 0; i < 2 * numPoints; i++) {
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float x = centerX + (float) (radius * Math.cos(i * angle - Math.PI / 2));
            float y = centerY + (float) (radius * Math.sin(i * angle - Math.PI / 2));
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        
        canvas.drawPath(path, paint);
    }
    
    private String getColorName(int red, int green, int blue) {
        // Simple color naming based on dominant color
        if (red > green && red > blue) {
            if (red > 200) return "Bright Red";
            if (red > 150) return "Red";
            return "Dark Red";
        } else if (green > red && green > blue) {
            if (green > 200) return "Bright Green";
            if (green > 150) return "Green";
            return "Dark Green";
        } else if (blue > red && blue > green) {
            if (blue > 200) return "Bright Blue";
            if (blue > 150) return "Blue";
            return "Dark Blue";
        } else if (red > 150 && green > 150 && blue < 100) {
            return "Yellow";
        } else if (red > 150 && blue > 150 && green < 100) {
            return "Magenta";
        } else if (green > 150 && blue > 150 && red < 100) {
            return "Cyan";
        } else if (red > 100 && green > 100 && blue > 100) {
            return "Light";
        } else {
            return "Dark";
        }
    }
}
