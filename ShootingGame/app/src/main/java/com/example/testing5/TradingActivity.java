package com.example.testing5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradingActivity extends AppCompatActivity {
    
    private static final String TAG = "TradingActivity";
    
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseService firebaseService;
    
    private String roomCode;
    private boolean isHost;
    private String otherPlayerId;
    private String otherPlayerName;
    
    // UI Components
    private TextView roomCodeText;
    private TextView confirmationStatusText;
    private TextView currencyTradeText;
    private TextView otherPlayerText;
    private Button leaveRoomButton;
    private GridView yourSkinsGridView;
    private GridView youGiveGridView;
    private GridView theyGiveGridView;
    private ImageView skinPreviewImage;
    private TextView skinNameText;
    private TextView skinDescriptionText;
    private TextView skinPriceText;
    private Button actionButton;
    private Button confirmTradeButton;
    private Button cancelTradeButton;
    private EditText youGiveCurrencyEditText;
    private EditText theyGiveCurrencyEditText;
    
    // Data
    private List<Skin> yourAvailableSkins;
    private List<Skin> yourTradeSkins;
    private List<Skin> theirTradeSkins;
    private int yourCurrencyOffer = 0;
    private int theirCurrencyOffer = 0;
    private int yourCurrentCurrency = 0;
    private Skin selectedSkin;
    
    // Adapters
    private SkinGridAdapter yourSkinsAdapter;
    private SkinGridAdapter youGiveAdapter;
    private SkinGridAdapter theyGiveAdapter;
    
    // Firebase listener
    private ListenerRegistration roomListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading);
        
        // Get room information from intent
        roomCode = getIntent().getStringExtra("roomCode");
        isHost = getIntent().getBooleanExtra("isHost", false);
        
        if (roomCode == null) {
            Toast.makeText(this, "Invalid room", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        initializeFirebase();
        setupRoomListener();
        loadYourSkins();
        loadUserCurrency();
    }
    
    private void initializeViews() {
        roomCodeText = findViewById(R.id.roomCodeText);
        confirmationStatusText = findViewById(R.id.confirmationStatusText);
        currencyTradeText = findViewById(R.id.currencyTradeText);
        otherPlayerText = findViewById(R.id.otherPlayerText);
        leaveRoomButton = findViewById(R.id.leaveRoomButton);
        yourSkinsGridView = findViewById(R.id.yourSkinsGridView);
        youGiveGridView = findViewById(R.id.youGiveGridView);
        theyGiveGridView = findViewById(R.id.theyGiveGridView);
        skinPreviewImage = findViewById(R.id.skinPreviewImage);
        skinNameText = findViewById(R.id.skinNameText);
        skinDescriptionText = findViewById(R.id.skinDescriptionText);
        skinPriceText = findViewById(R.id.skinPriceText);
        actionButton = findViewById(R.id.actionButton);
        confirmTradeButton = findViewById(R.id.confirmTradeButton);
        cancelTradeButton = findViewById(R.id.cancelTradeButton);
        youGiveCurrencyEditText = findViewById(R.id.youGiveCurrencyEditText);
        theyGiveCurrencyEditText = findViewById(R.id.theyGiveCurrencyEditText);
        
        roomCodeText.setText("Room: " + roomCode);
        
        leaveRoomButton.setOnClickListener(v -> leaveRoom());
        confirmTradeButton.setOnClickListener(v -> confirmTrade());
        cancelTradeButton.setOnClickListener(v -> cancelTrade());
        
        // Add currency change listeners
        youGiveCurrencyEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    int newValue = Integer.parseInt(s.toString().isEmpty() ? "0" : s.toString());
                    
                    // Validate currency amount
                    if (newValue > yourCurrentCurrency) {
                        Toast.makeText(TradingActivity.this, "Not enough currency! You have: " + yourCurrentCurrency, Toast.LENGTH_SHORT).show();
                        youGiveCurrencyEditText.setText(String.valueOf(yourCurrentCurrency));
                        newValue = yourCurrentCurrency;
                    }
                    
                    if (newValue != yourCurrencyOffer) {
                        yourCurrencyOffer = newValue;
                        updateCurrencyOffers();
                    }
                    updateCurrencyTradeDisplay();
                } catch (NumberFormatException e) {
                    if (yourCurrencyOffer != 0) {
                        yourCurrencyOffer = 0;
                        updateCurrencyOffers();
                    }
                }
            }
        });
        
        // theyGiveCurrencyEditText is read-only, no listener needed
        
        // Initialize data lists
        yourAvailableSkins = new ArrayList<>();
        yourTradeSkins = new ArrayList<>();
        theirTradeSkins = new ArrayList<>();
    }
    
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseService = FirebaseService.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    private void setupRoomListener() {
        // Listen for room changes
        roomListener = db.collection("tradingRooms").document(roomCode)
            .addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error listening to room", e);
                    return;
                }
                
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    updateRoomData(documentSnapshot);
                } else {
                    Toast.makeText(this, "Room no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }
    
    private void updateRoomData(DocumentSnapshot document) {
        String status = document.getString("status");
        String hostId = document.getString("hostId");
        String guestId = document.getString("guestId");
        String hostName = document.getString("hostName");
        String guestName = document.getString("guestName");
        
        // Determine other player info
        if (isHost && guestId != null) {
            otherPlayerId = guestId;
            otherPlayerName = guestName;
            otherPlayerText.setText("Guest: " + otherPlayerName);
        } else if (!isHost && hostId != null) {
            otherPlayerId = hostId;
            otherPlayerName = hostName;
            otherPlayerText.setText("Host: " + otherPlayerName);
        } else {
            otherPlayerText.setText("Waiting for player...");
        }
        
        // Update trade data
        if ("trading".equals(status)) {
            updateTradeData(document);
        } else if ("completed".equals(status)) {
            Toast.makeText(this, "Trade completed!", Toast.LENGTH_SHORT).show();
            finish();
        } else if ("cancelled".equals(status)) {
            Toast.makeText(this, "Trade cancelled", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void updateTradeData(DocumentSnapshot document) {
        // Get trade offers
        List<String> hostOffers = (List<String>) document.get("hostOffers");
        List<String> guestOffers = (List<String>) document.get("guestOffers");
        
        if (hostOffers == null) hostOffers = new ArrayList<>();
        if (guestOffers == null) guestOffers = new ArrayList<>();
        
        // Update trade grids based on role
        if (isHost) {
            updateTradeGrid(hostOffers, yourTradeSkins, youGiveAdapter);
            updateTradeGrid(guestOffers, theirTradeSkins, theyGiveAdapter);
        } else {
            updateTradeGrid(guestOffers, yourTradeSkins, youGiveAdapter);
            updateTradeGrid(hostOffers, theirTradeSkins, theyGiveAdapter);
        }
        
        // Update currency offers
        Long hostCurrencyOffer = document.getLong("hostCurrencyOffer");
        Long guestCurrencyOffer = document.getLong("guestCurrencyOffer");
        
        if (hostCurrencyOffer == null) hostCurrencyOffer = 0L;
        if (guestCurrencyOffer == null) guestCurrencyOffer = 0L;
        
        if (isHost) {
            int newTheirOffer = guestCurrencyOffer.intValue();
            if (newTheirOffer != theirCurrencyOffer) {
                theirCurrencyOffer = newTheirOffer;
                theyGiveCurrencyEditText.setText(String.valueOf(theirCurrencyOffer));
            }
        } else {
            int newTheirOffer = hostCurrencyOffer.intValue();
            if (newTheirOffer != theirCurrencyOffer) {
                theirCurrencyOffer = newTheirOffer;
                theyGiveCurrencyEditText.setText(String.valueOf(theirCurrencyOffer));
            }
        }
        
        // Update currency display in header
        updateCurrencyTradeDisplay();
        
        // Update confirm button state
        boolean hostConfirmed = Boolean.TRUE.equals(document.getBoolean("hostConfirmed"));
        boolean guestConfirmed = Boolean.TRUE.equals(document.getBoolean("guestConfirmed"));
        
        // Enable confirm button if user hasn't confirmed yet
        boolean userConfirmed = isHost ? hostConfirmed : guestConfirmed;
        confirmTradeButton.setEnabled(!userConfirmed);
        
        // Update button text based on confirmation status
        if (userConfirmed) {
            confirmTradeButton.setText("CONFIRMED");
        } else {
            confirmTradeButton.setText("CONFIRM TRADE");
        }
        
        // Update cancel button state - only enabled if user has confirmed
        cancelTradeButton.setEnabled(userConfirmed);
        
        // Update confirmation status text
        updateConfirmationStatus(hostConfirmed, guestConfirmed);
        
        // Check if both users have confirmed
        if (hostConfirmed && guestConfirmed) {
            executeTrade();
        }
    }
    
    private void updateConfirmationStatus(boolean hostConfirmed, boolean guestConfirmed) {
        if (otherPlayerId == null) {
            // No other player yet
            confirmationStatusText.setVisibility(View.GONE);
            currencyTradeText.setVisibility(View.GONE);
            return;
        }
        
        boolean otherPlayerConfirmed = isHost ? guestConfirmed : hostConfirmed;
        
        if (otherPlayerConfirmed) {
            confirmationStatusText.setText("✓ " + otherPlayerName + " confirmed");
            confirmationStatusText.setTextColor(0xFF00FF00); // Green
            confirmationStatusText.setVisibility(View.VISIBLE);
        } else {
            confirmationStatusText.setText("⏳ " + otherPlayerName + " confirming...");
            confirmationStatusText.setTextColor(0xFFFFD700); // Gold
            confirmationStatusText.setVisibility(View.VISIBLE);
        }
        
        // Update currency trade display
        updateCurrencyTradeDisplay();
    }
    
    private void updateCurrencyTradeDisplay() {
        if (yourCurrencyOffer > 0 || theirCurrencyOffer > 0) {
            String currencyText = "💰 " + yourCurrencyOffer + " ↔ " + theirCurrencyOffer;
            currencyTradeText.setText(currencyText);
            currencyTradeText.setVisibility(View.VISIBLE);
        } else {
            currencyTradeText.setVisibility(View.GONE);
        }
    }
    
    private void updateTradeGrid(List<String> skinIds, List<Skin> skinList, SkinGridAdapter adapter) {
        if (skinIds.isEmpty()) {
            skinList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }
        
        // Load skins by IDs
        firebaseService.getSkinsByIds(skinIds, new OnCompleteListener<List<Skin>>() {
            @Override
            public void onComplete(@NonNull Task<List<Skin>> task) {
                if (task.isSuccessful()) {
                    skinList.clear();
                    skinList.addAll(task.getResult());
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
    
    private void loadUserCurrency() {
        // Load user's current currency
        firebaseService.getUserById(currentUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DocumentSnapshot document = task.getResult();
                    Long currency = document.getLong("currency");
                    yourCurrentCurrency = currency != null ? currency.intValue() : 0;
                }
            }
        });
    }
    
    private void loadYourSkins() {
        // Load user's skins that are available for trading
        firebaseService.getUserOwnedSkins(currentUser.getUid(), new OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<com.google.firebase.firestore.QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    yourAvailableSkins.clear();
                    
                    // Get user's selected skin to exclude it from trading
                    firebaseService.getUserById(currentUser.getUid(), new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> userTask) {
                            String selectedSkinId = null;
                            if (userTask.isSuccessful() && userTask.getResult().exists()) {
                                selectedSkinId = userTask.getResult().getString("selectedSkin");
                            }
                            
                            // Filter skins for trading
                            for (com.google.firebase.firestore.DocumentSnapshot document : task.getResult()) {
                                Skin skin = document.toObject(Skin.class);
                                if (skin != null && !skin.isForSale() && !skin.getSkinId().equals(selectedSkinId)) {
                                    yourAvailableSkins.add(skin);
                                }
                            }
                            setupYourSkinsGrid();
                        }
                    });
                } else {
                    Log.e(TAG, "Error loading your skins", task.getException());
                    Toast.makeText(TradingActivity.this, "Error loading your skins", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupYourSkinsGrid() {
        yourSkinsAdapter = new SkinGridAdapter(this, yourAvailableSkins, null, currentUser.getUid());
        yourSkinsAdapter.setOnSkinClickListener(new SkinGridAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedSkin = skin;
                showSkinDetails(skin, true);
            }
        });
        yourSkinsGridView.setAdapter(yourSkinsAdapter);
        
        // Setup trade grids
        youGiveAdapter = new SkinGridAdapter(this, yourTradeSkins, null, currentUser.getUid());
        youGiveAdapter.setOnSkinClickListener(new SkinGridAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedSkin = skin;
                showSkinDetails(skin, true); // Show as your skin for retrieval
            }
        });
        youGiveGridView.setAdapter(youGiveAdapter);
        
        theyGiveAdapter = new SkinGridAdapter(this, theirTradeSkins, null, currentUser.getUid());
        theyGiveAdapter.setOnSkinClickListener(new SkinGridAdapter.OnSkinClickListener() {
            @Override
            public void onSkinClick(Skin skin) {
                selectedSkin = skin;
                showSkinDetails(skin, false); // Show as their skin (no buttons)
            }
        });
        theyGiveGridView.setAdapter(theyGiveAdapter);
    }
    
    private void showSkinDetails(Skin skin, boolean isYourSkin) {
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setSkinToImageView(skinPreviewImage, skin.getImageBase64());
        }
        
        skinNameText.setText(skin.getName());
        skinDescriptionText.setText(skin.getDescription());
        skinPriceText.setText("Price: " + skin.getPrice() + " coins");
        
        if (isYourSkin) {
            // Check if skin is already in trade by ID
            boolean isInTrade = false;
            for (Skin tradeSkin : yourTradeSkins) {
                if (tradeSkin.getSkinId().equals(skin.getSkinId())) {
                    isInTrade = true;
                    break;
                }
            }
            
            if (isInTrade) {
                actionButton.setText("RETRIEVE FROM TRADE");
                actionButton.setOnClickListener(v -> removeFromTrade(skin));
            } else {
                actionButton.setText("PUT TO TRADE");
                actionButton.setOnClickListener(v -> addToTrade(skin));
            }
            actionButton.setVisibility(View.VISIBLE);
        } else {
            actionButton.setVisibility(View.GONE);
        }
    }
    
    private void addToTrade(Skin skin) {
        // Check if skin is already in trade by ID
        for (Skin tradeSkin : yourTradeSkins) {
            if (tradeSkin.getSkinId().equals(skin.getSkinId())) {
                Toast.makeText(this, "Skin already in trade", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        yourTradeSkins.add(skin);
        youGiveAdapter.notifyDataSetChanged();
        updateTradeOffers();
        
        Toast.makeText(this, "Added to trade", Toast.LENGTH_SHORT).show();
    }
    
    private void removeFromTrade(Skin skin) {
        // Remove skin by ID
        for (int i = yourTradeSkins.size() - 1; i >= 0; i--) {
            if (yourTradeSkins.get(i).getSkinId().equals(skin.getSkinId())) {
                yourTradeSkins.remove(i);
                break;
            }
        }
        youGiveAdapter.notifyDataSetChanged();
        updateTradeOffers();
        
        Toast.makeText(this, "Removed from trade", Toast.LENGTH_SHORT).show();
    }
    
    private void updateTradeOffers() {
        List<String> skinIds = new ArrayList<>();
        for (Skin skin : yourTradeSkins) {
            skinIds.add(skin.getSkinId());
        }
        
        Map<String, Object> updates = new HashMap<>();
        if (isHost) {
            updates.put("hostOffers", skinIds);
            updates.put("hostCurrencyOffer", yourCurrencyOffer);
            updates.put("hostConfirmed", false); // Reset current user's confirmation
            updates.put("guestConfirmed", false); // Reset other user's confirmation too
        } else {
            updates.put("guestOffers", skinIds);
            updates.put("guestCurrencyOffer", yourCurrencyOffer);
            updates.put("guestConfirmed", false); // Reset current user's confirmation
            updates.put("hostConfirmed", false); // Reset other user's confirmation too
        }
        
        db.collection("tradingRooms").document(roomCode).update(updates);
    }
    
    private void updateCurrencyOffers() {
        Map<String, Object> updates = new HashMap<>();
        if (isHost) {
            updates.put("hostCurrencyOffer", yourCurrencyOffer);
            updates.put("hostConfirmed", false); // Reset current user's confirmation
            updates.put("guestConfirmed", false); // Reset other user's confirmation too
        } else {
            updates.put("guestCurrencyOffer", yourCurrencyOffer);
            updates.put("guestConfirmed", false); // Reset current user's confirmation
            updates.put("hostConfirmed", false); // Reset other user's confirmation too
        }
        
        db.collection("tradingRooms").document(roomCode).update(updates);
    }
    
    private void confirmTrade() {
        Map<String, Object> updates = new HashMap<>();
        if (isHost) {
            updates.put("hostConfirmed", true);
        } else {
            updates.put("guestConfirmed", true);
        }
        
        db.collection("tradingRooms").document(roomCode).update(updates);
        Toast.makeText(this, "Trade confirmed", Toast.LENGTH_SHORT).show();
    }
    
    private void executeTrade() {
        // Execute the actual trade - exchange skins and/or currency between users
        if (yourTradeSkins.isEmpty() && theirTradeSkins.isEmpty() && yourCurrencyOffer == 0 && theirCurrencyOffer == 0) {
            Toast.makeText(this, "Nothing to trade", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update skin ownership and currency in Firebase
        firebaseService.executeTrade(currentUser.getUid(), otherPlayerId, yourTradeSkins, theirTradeSkins, yourCurrencyOffer, theirCurrencyOffer, new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                if (task.isSuccessful()) {
                    // Mark trade as completed
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "completed");
                    db.collection("tradingRooms").document(roomCode).update(updates);
                    
                    Toast.makeText(TradingActivity.this, "Trade completed successfully!", Toast.LENGTH_LONG).show();
                    
                    // Close the trading activity after a short delay
                    new android.os.Handler().postDelayed(() -> finish(), 2000);
                } else {
                    Toast.makeText(TradingActivity.this, "Trade failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void cancelTrade() {
        // Reset user's confirmation status
        Map<String, Object> updates = new HashMap<>();
        if (isHost) {
            updates.put("hostConfirmed", false);
        } else {
            updates.put("guestConfirmed", false);
        }
        
        db.collection("tradingRooms").document(roomCode).update(updates);
        Toast.makeText(this, "Confirmation cancelled", Toast.LENGTH_SHORT).show();
    }
    
    private void leaveRoom() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        
        db.collection("tradingRooms").document(roomCode).update(updates);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null) {
            roomListener.remove();
        }
    }
}
