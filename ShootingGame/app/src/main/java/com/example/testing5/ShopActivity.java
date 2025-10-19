package com.example.testing5;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ShopActivity extends AppCompatActivity {
    
    private static final String TAG = "ShopActivity";
    
    // UI Components
    private Button backButton;
    private Button buyTabButton;
    private Button sellTabButton;
    private Button retrieveTabButton;
    private LinearLayout buyTabContent;
    private LinearLayout sellTabContent;
    private LinearLayout retrieveTabContent;
    
    // Buy Tab Components
    private GridView buySkinsGridView;
    private LinearLayout buyDetailsPanel;
    private ImageView buySkinPreviewImage;
    private TextView buySkinNameText;
    private TextView buySkinPriceText;
    private Button buySkinButton;
    
    // Sell Tab Components
    private GridView sellSkinsGridView;
    private LinearLayout sellDetailsPanel;
    private ImageView sellSkinPreviewImage;
    private TextView sellSkinNameText;
    private EditText sellPriceEditText;
    private Button putToShopButton;
    
    // Retrieve Tab Components
    private GridView retrieveSkinsGridView;
    private LinearLayout retrieveDetailsPanel;
    private ImageView retrieveSkinPreviewImage;
    private TextView retrieveSkinNameText;
    private TextView retrieveSkinPriceText;
    private Button retrieveSkinButton;
    
    // Data
    private FirebaseService firebaseService;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private User userProfile;
    
    // Buy Tab Data
    private List<Skin> availableSkins;
    private Skin selectedBuySkin;
    private ShopBuyAdapter buyAdapter;
    
    // Sell Tab Data
    private List<Skin> userSkins;
    private Skin selectedSellSkin;
    private ShopSellAdapter sellAdapter;
    
    // Retrieve Tab Data
    private List<Skin> retrieveSkins;
    private Skin selectedRetrieveSkin;
    private ShopRetrieveAdapter retrieveAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        
        initializeViews();
        initializeFirebase();
        loadUserData();
        setupTabButtons();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        buyTabButton = findViewById(R.id.buyTabButton);
        sellTabButton = findViewById(R.id.sellTabButton);
        retrieveTabButton = findViewById(R.id.retrieveTabButton);
        buyTabContent = findViewById(R.id.buyTabContent);
        sellTabContent = findViewById(R.id.sellTabContent);
        retrieveTabContent = findViewById(R.id.retrieveTabContent);
        
        // Buy Tab
        buySkinsGridView = findViewById(R.id.buySkinsGridView);
        buyDetailsPanel = findViewById(R.id.buyDetailsPanel);
        buySkinPreviewImage = findViewById(R.id.buySkinPreviewImage);
        buySkinNameText = findViewById(R.id.buySkinNameText);
        buySkinPriceText = findViewById(R.id.buySkinPriceText);
        buySkinButton = findViewById(R.id.buySkinButton);
        
        // Sell Tab
        sellSkinsGridView = findViewById(R.id.sellSkinsGridView);
        sellDetailsPanel = findViewById(R.id.sellDetailsPanel);
        sellSkinPreviewImage = findViewById(R.id.sellSkinPreviewImage);
        sellSkinNameText = findViewById(R.id.sellSkinNameText);
        sellPriceEditText = findViewById(R.id.sellPriceEditText);
        putToShopButton = findViewById(R.id.putToShopButton);
        
        // Retrieve Tab
        retrieveSkinsGridView = findViewById(R.id.retrieveSkinsGridView);
        retrieveDetailsPanel = findViewById(R.id.retrieveDetailsPanel);
        retrieveSkinPreviewImage = findViewById(R.id.retrieveSkinPreviewImage);
        retrieveSkinNameText = findViewById(R.id.retrieveSkinNameText);
        retrieveSkinPriceText = findViewById(R.id.retrieveSkinPriceText);
        retrieveSkinButton = findViewById(R.id.retrieveSkinButton);
        
        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        buySkinButton.setOnClickListener(v -> buySelectedSkin());
        putToShopButton.setOnClickListener(v -> putSkinToShop());
        retrieveSkinButton.setOnClickListener(v -> retrieveSelectedSkin());
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
        
        currentUser = firebaseUser;
        
        // Load user profile
        firebaseService.getUserProfile(firebaseUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        userProfile = document.toObject(User.class);
                        if (userProfile != null) {
                            loadBuySkins();
                            loadSellSkins();
                            loadRetrieveSkins();
                        }
                    }
                } else {
                    Log.e(TAG, "Error loading user profile", task.getException());
                    Toast.makeText(ShopActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupTabButtons() {
        buyTabButton.setOnClickListener(v -> showBuyTab());
        sellTabButton.setOnClickListener(v -> showSellTab());
        retrieveTabButton.setOnClickListener(v -> showRetrieveTab());
        
        // Show buy tab by default
        showBuyTab();
    }
    
    private void showBuyTab() {
        buyTabContent.setVisibility(View.VISIBLE);
        sellTabContent.setVisibility(View.GONE);
        retrieveTabContent.setVisibility(View.GONE);
        buyTabButton.setBackgroundResource(R.drawable.tab_button_background);
        sellTabButton.setBackgroundResource(R.drawable.tab_button_background);
        retrieveTabButton.setBackgroundResource(R.drawable.tab_button_background);
    }
    
    private void showSellTab() {
        buyTabContent.setVisibility(View.GONE);
        sellTabContent.setVisibility(View.VISIBLE);
        retrieveTabContent.setVisibility(View.GONE);
        buyTabButton.setBackgroundResource(R.drawable.tab_button_background);
        sellTabButton.setBackgroundResource(R.drawable.tab_button_background);
        retrieveTabButton.setBackgroundResource(R.drawable.tab_button_background);
    }
    
    private void showRetrieveTab() {
        buyTabContent.setVisibility(View.GONE);
        sellTabContent.setVisibility(View.GONE);
        retrieveTabContent.setVisibility(View.VISIBLE);
        buyTabButton.setBackgroundResource(R.drawable.tab_button_background);
        sellTabButton.setBackgroundResource(R.drawable.tab_button_background);
        retrieveTabButton.setBackgroundResource(R.drawable.tab_button_background);
    }
    
    private void loadBuySkins() {
        // Load skins that are for sale (not owned by current user)
        firebaseService.getSkinsForSale(currentUser.getUid(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    availableSkins = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Skin skin = document.toObject(Skin.class);
                        if (skin != null && skin.isForSale() && !skin.getCurrentOwner().equals(currentUser.getUid())) {
                            availableSkins.add(skin);
                        }
                    }
                    setupBuyGrid();
                } else {
                    Log.e(TAG, "Error loading available skins", task.getException());
                    Toast.makeText(ShopActivity.this, "Error loading available skins", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadSellSkins() {
        // Load user's skins that are not currently selected for game and not for sale
        firebaseService.getUserOwnedSkins(currentUser.getUid(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    userSkins = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Skin skin = document.toObject(Skin.class);
                        if (skin != null && !skin.isForSale() && !skin.getSkinId().equals(userProfile.getSelectedSkin())) {
                            userSkins.add(skin);
                        }
                    }
                    setupSellGrid();
                } else {
                    Log.e(TAG, "Error loading user skins", task.getException());
                    Toast.makeText(ShopActivity.this, "Error loading user skins", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadRetrieveSkins() {
        // Load user's skins that are currently for sale (can be retrieved)
        firebaseService.getUserOwnedSkins(currentUser.getUid(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    retrieveSkins = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Skin skin = document.toObject(Skin.class);
                        if (skin != null && skin.isForSale()) {
                            retrieveSkins.add(skin);
                        }
                    }
                    setupRetrieveGrid();
                } else {
                    Log.e(TAG, "Error loading retrieve skins", task.getException());
                    Toast.makeText(ShopActivity.this, "Error loading retrieve skins", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupBuyGrid() {
        buyAdapter = new ShopBuyAdapter(this, availableSkins);
        buyAdapter.setOnSkinClickListener(new ShopBuyAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedBuySkin = skin;
                showBuySkinDetails(skin);
            }
        });
        buySkinsGridView.setAdapter(buyAdapter);
    }
    
    private void setupSellGrid() {
        sellAdapter = new ShopSellAdapter(this, userSkins);
        sellAdapter.setOnSkinClickListener(new ShopSellAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedSellSkin = skin;
                showSellSkinDetails(skin);
            }
        });
        sellSkinsGridView.setAdapter(sellAdapter);
    }
    
    private void setupRetrieveGrid() {
        retrieveAdapter = new ShopRetrieveAdapter(this, retrieveSkins);
        retrieveAdapter.setOnSkinClickListener(new ShopRetrieveAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedRetrieveSkin = skin;
                showRetrieveSkinDetails(skin);
            }
        });
        retrieveSkinsGridView.setAdapter(retrieveAdapter);
    }
    
    private void showBuySkinDetails(Skin skin) {
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setSkinToImageView(buySkinPreviewImage, skin.getImageBase64());
        }
        
        buySkinNameText.setText(skin.getName());
        buySkinPriceText.setText("Price: " + skin.getPrice() + " coins");
        
        // Check if user has enough currency
        if (userProfile.getCurrency() >= skin.getPrice()) {
            buySkinButton.setEnabled(true);
            buySkinButton.setText("BUY SKIN");
        } else {
            buySkinButton.setEnabled(false);
            buySkinButton.setText("NOT ENOUGH COINS");
        }
        
        buyDetailsPanel.setVisibility(View.VISIBLE);
    }
    
    private void showSellSkinDetails(Skin skin) {
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setSkinToImageView(sellSkinPreviewImage, skin.getImageBase64());
        }
        
        sellSkinNameText.setText(skin.getName());
        sellPriceEditText.setText("");
        
        sellDetailsPanel.setVisibility(View.VISIBLE);
    }
    
    private void buySelectedSkin() {
        if (selectedBuySkin == null) return;
        
        if (userProfile.getCurrency() < selectedBuySkin.getPrice()) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // First check if the skin is still for sale (not retrieved)
        firebaseService.getSkinById(selectedBuySkin.getSkinId(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Skin currentSkin = document.toObject(Skin.class);
                        if (currentSkin != null) {
                            // Check if skin is still for sale
                            if (!currentSkin.isForSale()) {
                                Toast.makeText(ShopActivity.this, "The skin has already been retrieved", Toast.LENGTH_SHORT).show();
                                // Refresh buy skins to remove retrieved skin
                                loadBuySkins();
                                return;
                            }
                            
                            // Check if skin is still owned by the same seller
                            if (!currentSkin.getCurrentOwner().equals(selectedBuySkin.getCurrentOwner())) {
                                Toast.makeText(ShopActivity.this, "The skin has already been retrieved", Toast.LENGTH_SHORT).show();
                                // Refresh buy skins to remove retrieved skin
                                loadBuySkins();
                                return;
                            }
                            
                            // Skin is still available for purchase, proceed with purchase
                            purchaseSkinInternal();
                        }
                    } else {
                        Toast.makeText(ShopActivity.this, "The skin has already been retrieved", Toast.LENGTH_SHORT).show();
                        // Refresh buy skins to remove retrieved skin
                        loadBuySkins();
                    }
                } else {
                    Log.e(TAG, "Error checking skin status", task.getException());
                    Toast.makeText(ShopActivity.this, "Error checking skin status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void purchaseSkinInternal() {
        // Purchase the skin
        firebaseService.purchaseSkin(selectedBuySkin.getSkinId(), currentUser.getUid(), new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful() && task.getResult()) {
                    Toast.makeText(ShopActivity.this, "Skin purchased successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Update user currency
                    userProfile.setCurrency(userProfile.getCurrency() - selectedBuySkin.getPrice());
                    
                    // Refresh the grids
                    loadBuySkins();
                    loadSellSkins();
                    loadRetrieveSkins();
                    
                    // Hide details panel
                    buyDetailsPanel.setVisibility(View.GONE);
                    selectedBuySkin = null;
                } else {
                    Log.e(TAG, "Error purchasing skin", task.getException());
                    Toast.makeText(ShopActivity.this, "Error purchasing skin", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void putSkinToShop() {
        if (selectedSellSkin == null) return;
        
        String priceText = sellPriceEditText.getText().toString().trim();
        if (priceText.isEmpty()) {
            Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int price = Integer.parseInt(priceText);
            if (price <= 0) {
                Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update skin to be for sale
            selectedSellSkin.setForSale(true);
            selectedSellSkin.setPrice(price);
            
            firebaseService.updateSkin(selectedSellSkin, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(ShopActivity.this, "Skin put up for sale!", Toast.LENGTH_SHORT).show();
                        
                        // Refresh the grids
                        loadBuySkins();
                        loadSellSkins();
                        loadRetrieveSkins();
                        
                        // Hide details panel
                        sellDetailsPanel.setVisibility(View.GONE);
                        selectedSellSkin = null;
                    } else {
                        Log.e(TAG, "Error putting skin for sale", task.getException());
                        Toast.makeText(ShopActivity.this, "Error putting skin for sale", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showRetrieveSkinDetails(Skin skin) {
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setSkinToImageView(retrieveSkinPreviewImage, skin.getImageBase64());
        }
        
        retrieveSkinNameText.setText(skin.getName());
        retrieveSkinPriceText.setText("Price: " + skin.getPrice() + " coins");
        
        retrieveSkinButton.setEnabled(true);
        retrieveSkinButton.setText("RETRIEVE SKIN");
        
        retrieveDetailsPanel.setVisibility(View.VISIBLE);
    }
    
    private void retrieveSelectedSkin() {
        if (selectedRetrieveSkin == null) return;
        
        // First check if the skin is still for sale (not sold while user was trying to retrieve)
        firebaseService.getSkinById(selectedRetrieveSkin.getSkinId(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Skin currentSkin = document.toObject(Skin.class);
                        if (currentSkin != null) {
                            // Check if skin is still for sale and still owned by current user
                            if (!currentSkin.isForSale()) {
                                Toast.makeText(ShopActivity.this, "Skin has already been sold", Toast.LENGTH_SHORT).show();
                                // Refresh retrieve skins to remove sold skin
                                loadRetrieveSkins();
                                return;
                            }
                            
                            if (!currentSkin.getCurrentOwner().equals(currentUser.getUid())) {
                                Toast.makeText(ShopActivity.this, "Skin has already been sold", Toast.LENGTH_SHORT).show();
                                // Refresh retrieve skins to remove sold skin
                                loadRetrieveSkins();
                                return;
                            }
                            
                            // Skin is still available for retrieval, proceed with retrieval
                            currentSkin.setForSale(false);
                            
                            firebaseService.updateSkin(currentSkin, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ShopActivity.this, "Skin retrieved successfully!", Toast.LENGTH_SHORT).show();
                                        
                                        // Refresh the grids
                                        loadBuySkins();
                                        loadSellSkins();
                                        loadRetrieveSkins();
                                        
                                        // Hide details panel
                                        retrieveDetailsPanel.setVisibility(View.GONE);
                                        selectedRetrieveSkin = null;
                                    } else {
                                        Log.e(TAG, "Error retrieving skin", task.getException());
                                        Toast.makeText(ShopActivity.this, "Error retrieving skin", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    } else {
                        Toast.makeText(ShopActivity.this, "Skin has already been sold", Toast.LENGTH_SHORT).show();
                        // Refresh retrieve skins to remove sold skin
                        loadRetrieveSkins();
                    }
                } else {
                    Log.e(TAG, "Error checking skin status", task.getException());
                    Toast.makeText(ShopActivity.this, "Error checking skin status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
