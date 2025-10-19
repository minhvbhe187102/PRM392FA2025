package com.example.testing5;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {
    private static final String TAG = "InventoryActivity";
    
    private GridView skinGridView;
    private ImageView skinPreviewImage;
    private TextView skinNameText;
    private TextView skinDescriptionText;
    private TextView skinPriceText;
    private TextView skinOwnerText;
    private TextView skinStatusText;
    private Button equipSkinButton;
    private Button purchaseSkinButton;
    private Button backButton;
    
    private SkinGridAdapter skinAdapter;
    private List<Skin> allSkins;
    private User currentUser;
    private Skin selectedSkin;
    private FirebaseService firebaseService;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        
        initializeViews();
        initializeFirebase();
        loadUserData();
    }
    
    private void initializeViews() {
        skinGridView = findViewById(R.id.skinGridView);
        skinPreviewImage = findViewById(R.id.skinPreviewImage);
        skinNameText = findViewById(R.id.skinNameText);
        skinDescriptionText = findViewById(R.id.skinDescriptionText);
        skinPriceText = findViewById(R.id.skinPriceText);
        skinOwnerText = findViewById(R.id.skinOwnerText);
        skinStatusText = findViewById(R.id.skinStatusText);
        equipSkinButton = findViewById(R.id.equipSkinButton);
        purchaseSkinButton = findViewById(R.id.purchaseSkinButton);
        backButton = findViewById(R.id.backButton);
        
        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        
        equipSkinButton.setOnClickListener(v -> equipSelectedSkin());
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
                        loadAllSkins();
                    } else {
                        Toast.makeText(InventoryActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Log.e(TAG, "Error loading user profile", task.getException());
                    Toast.makeText(InventoryActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
    
    private void loadAllSkins() {
        // Load only skins owned by the current user
        firebaseService.getUserOwnedSkins(currentUser.getUserId(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allSkins = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Skin skin = document.toObject(Skin.class);
                        if (skin != null) {
                            allSkins.add(skin);
                        }
                    }
                    setupSkinGrid();
                } else {
                    Log.e(TAG, "Error loading owned skins", task.getException());
                    Toast.makeText(InventoryActivity.this, "Error loading owned skins", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupSkinGrid() {
        skinAdapter = new SkinGridAdapter(this, allSkins, currentUser.getSelectedSkin(), currentUser.getUserId());
        skinAdapter.setOnSkinClickListener(new SkinGridAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedSkin = skin;
                showSkinDetails(skin);
            }
        });
        skinGridView.setAdapter(skinAdapter);
    }
    
    private void showSkinDetails(Skin skin) {
        // Update skin preview
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setImageFromBase64(skinPreviewImage, skin.getImageBase64());
        }
        
        // Update skin info
        skinNameText.setText(skin.getName());
        skinDescriptionText.setText(skin.getDescription());
        skinPriceText.setText("Price: " + skin.getPrice() + " coins");
        skinOwnerText.setText("Owner: You");
        
        // Check if currently equipped
        boolean isEquipped = skin.getSkinId().equals(currentUser.getSelectedSkin());
        
        if (isEquipped) {
            skinStatusText.setText("Currently Selected");
            skinStatusText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            equipSkinButton.setVisibility(View.GONE);
        } else {
            skinStatusText.setText("Owned - Ready to Select");
            skinStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            equipSkinButton.setVisibility(View.VISIBLE);
        }
        
        // Hide purchase button since all skins shown are owned
        purchaseSkinButton.setVisibility(View.GONE);
    }
    
    private void equipSelectedSkin() {
        if (selectedSkin == null) return;
        
        // Update user's selected skin
        currentUser.setSelectedSkin(selectedSkin.getSkinId());
        
        firebaseService.updateUserProfile(currentUser, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(InventoryActivity.this, "Skin selected for game!", Toast.LENGTH_SHORT).show();
                    skinAdapter.updateSelectedSkin(selectedSkin.getSkinId());
                    showSkinDetails(selectedSkin); // Refresh details
                } else {
                    Log.e(TAG, "Error selecting skin", task.getException());
                    Toast.makeText(InventoryActivity.this, "Error selecting skin", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
}
