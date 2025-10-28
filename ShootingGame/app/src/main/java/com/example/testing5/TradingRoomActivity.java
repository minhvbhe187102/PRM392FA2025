package com.example.testing5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TradingRoomActivity extends AppCompatActivity {
    
    private static final String TAG = "TradingRoomActivity";
    
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    
    private Button createRoomButton;
    private Button joinRoomButton;
    private Button backButton;
    private EditText roomCodeEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading_room);
        
        initializeViews();
        initializeFirebase();
    }
    
    private void initializeViews() {
        createRoomButton = findViewById(R.id.createRoomButton);
        joinRoomButton = findViewById(R.id.joinRoomButton);
        backButton = findViewById(R.id.backButton);
        roomCodeEditText = findViewById(R.id.roomCodeEditText);
        
        createRoomButton.setOnClickListener(v -> createRoom());
        joinRoomButton.setOnClickListener(v -> joinRoom());
        backButton.setOnClickListener(v -> finish());
    }
    
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    private void createRoom() {
        // Generate a 6-digit room code
        String roomCode = generateRoomCode();
        
        // Create room document
        Map<String, Object> room = new HashMap<>();
        room.put("roomCode", roomCode);
        room.put("hostId", currentUser.getUid());
        room.put("hostName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Player");
        room.put("guestId", null);
        room.put("guestName", null);
        room.put("status", "waiting");
        room.put("createdAt", System.currentTimeMillis());
        room.put("expiresAt", System.currentTimeMillis() + (15 * 60 * 1000)); // 15 minutes
        room.put("hostOffers", new ArrayList<String>());
        room.put("guestOffers", new ArrayList<String>());
        room.put("hostConfirmed", false);
        room.put("guestConfirmed", false);
        
        db.collection("tradingRooms").document(roomCode).set(room)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(TradingRoomActivity.this, "Room created! Code: " + roomCode, Toast.LENGTH_LONG).show();
                        
                        // Navigate to trading activity
                        Intent intent = new Intent(TradingRoomActivity.this, TradingActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("isHost", true);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Error creating room", task.getException());
                        Toast.makeText(TradingRoomActivity.this, "Error creating room", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    private void joinRoom() {
        String roomCode = roomCodeEditText.getText().toString().trim();
        
        if (roomCode.isEmpty()) {
            Toast.makeText(this, "Please enter a room code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (roomCode.length() != 6) {
            Toast.makeText(this, "Room code must be 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if room exists and has space
        db.collection("tradingRooms").document(roomCode).get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String hostId = document.getString("hostId");
                            String guestId = document.getString("guestId");
                            String status = document.getString("status");
                            
                            // Check if room is available
                            if (hostId != null && guestId == null && "waiting".equals(status)) {
                                // Check if user is not trying to join their own room
                                if (!hostId.equals(currentUser.getUid())) {
                                    // Join the room
                                    joinRoomInternal(roomCode);
                                } else {
                                    Toast.makeText(TradingRoomActivity.this, "Cannot join your own room", Toast.LENGTH_SHORT).show();
                                }
                            } else if (hostId != null && guestId != null) {
                                Toast.makeText(TradingRoomActivity.this, "Room is full", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(TradingRoomActivity.this, "Room not available", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(TradingRoomActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error checking room", task.getException());
                        Toast.makeText(TradingRoomActivity.this, "Error checking room", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    private void joinRoomInternal(String roomCode) {
        // Update room with guest information
        Map<String, Object> updates = new HashMap<>();
        updates.put("guestId", currentUser.getUid());
        updates.put("guestName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Player");
        updates.put("status", "trading");
        
        db.collection("tradingRooms").document(roomCode).update(updates)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(TradingRoomActivity.this, "Joined room successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to trading activity
                        Intent intent = new Intent(TradingRoomActivity.this, TradingActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("isHost", false);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Error joining room", task.getException());
                        Toast.makeText(TradingRoomActivity.this, "Error joining room", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    private String generateRoomCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // 100000 to 999999
        return String.valueOf(code);
    }
}
