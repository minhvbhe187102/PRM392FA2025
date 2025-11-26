package com.example.testing5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MultiplayerLobbyActivity extends AppCompatActivity {
    
    private static final String TAG = "MultiplayerLobbyActivity";
    
    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference roomRef;
    private ValueEventListener roomListener;
    
    private String roomCode;
    private boolean isHost;
    private boolean isReady = false;
    
    private TextView roomCodeText;
    private TextView hostNameText;
    private TextView guestNameText;
    private TextView hostReadyText;
    private TextView guestReadyText;
    private TextView statusText;
    private Button readyButton;
    private Button leaveButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_lobby);
        
        // Get room info from intent
        roomCode = getIntent().getStringExtra("roomCode");
        isHost = getIntent().getBooleanExtra("isHost", false);
        
        initializeViews();
        initializeFirebase();
        setupRoomListener();
    }
    
    private void initializeViews() {
        roomCodeText = findViewById(R.id.roomCodeText);
        hostNameText = findViewById(R.id.hostNameText);
        guestNameText = findViewById(R.id.guestNameText);
        hostReadyText = findViewById(R.id.hostReadyText);
        guestReadyText = findViewById(R.id.guestReadyText);
        statusText = findViewById(R.id.statusText);
        readyButton = findViewById(R.id.readyButton);
        leaveButton = findViewById(R.id.leaveButton);
        
        roomCodeText.setText("Room Code: " + roomCode);
        
        readyButton.setOnClickListener(v -> toggleReady());
        leaveButton.setOnClickListener(v -> leaveRoom());
    }
    
    private void initializeFirebase() {
        database = FirebaseDatabase.getInstance("https://shootinggame-ff8aa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        roomRef = database.getReference("multiplayerRooms").child(roomCode);
    }
    
    private void setupRoomListener() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Room was deleted
                    Toast.makeText(MultiplayerLobbyActivity.this, "Room was closed", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                updateRoomData(snapshot);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Room listener cancelled", error.toException());
                Toast.makeText(MultiplayerLobbyActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        };
        
        roomRef.addValueEventListener(roomListener);
    }
    
    private void updateRoomData(DataSnapshot snapshot) {
        // Update player names
        String hostName = snapshot.child("hostName").getValue(String.class);
        String guestName = snapshot.child("guestName").getValue(String.class);
        String status = snapshot.child("status").getValue(String.class);
        
        Log.d(TAG, "Room data - Host: " + hostName + ", Guest: " + guestName + ", Status: " + status);
        
        hostNameText.setText(hostName != null ? hostName : "Host Player");
        
        if (guestName != null && !guestName.isEmpty()) {
            guestNameText.setText(guestName);
            Log.d(TAG, "Guest joined: " + guestName);
        } else {
            guestNameText.setText("Waiting for player...");
            Log.d(TAG, "No guest yet");
        }
        
        // Update ready status
        Boolean hostReady = snapshot.child("hostReady").getValue(Boolean.class);
        Boolean guestReady = snapshot.child("guestReady").getValue(Boolean.class);
        
        if (hostReady != null) {
            hostReadyText.setText(hostReady ? "READY" : "NOT READY");
            hostReadyText.setTextColor(hostReady ? getResources().getColor(android.R.color.holo_green_light) : getResources().getColor(android.R.color.holo_red_light));
        }
        
        if (guestReady != null) {
            guestReadyText.setText(guestReady ? "READY" : "NOT READY");
            guestReadyText.setTextColor(guestReady ? getResources().getColor(android.R.color.holo_green_light) : getResources().getColor(android.R.color.holo_red_light));
        }
        
        // Update status and ready button
        if (guestName == null || guestName.isEmpty()) {
            statusText.setText("Waiting for another player to join...");
            readyButton.setEnabled(false);
            readyButton.setText("WAITING FOR PLAYER");
            readyButton.setAlpha(0.5f); // Make button semi-transparent when disabled
        } else {
            readyButton.setEnabled(true);
            readyButton.setAlpha(1.0f); // Make button fully visible when enabled
            readyButton.setText(isReady ? "NOT READY" : "READY");
            
            if (hostReady != null && guestReady != null && hostReady && guestReady) {
                statusText.setText("Both players ready! Starting game...");
                startGame();
            } else {
                statusText.setText("Waiting for both players to be ready...");
            }
        }
    }
    
    private void toggleReady() {
        isReady = !isReady;
        
        String readyField = isHost ? "hostReady" : "guestReady";
        
        Map<String, Object> updates = new HashMap<>();
        updates.put(readyField, isReady);
        
        roomRef.updateChildren(updates)
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error updating ready status", task.getException());
                    Toast.makeText(this, "Error updating ready status", Toast.LENGTH_SHORT).show();
                    isReady = !isReady; // Revert on error
                }
            });
    }
    
    private void startGame() {
        // Update room status to indicate game has started
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "playing");
        updates.put("gameStarted", true);
        
        roomRef.updateChildren(updates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Navigate to multiplayer game
                    Intent intent = new Intent(MultiplayerLobbyActivity.this, MultiplayerGameActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("isHost", isHost);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "Error starting game", task.getException());
                    Toast.makeText(this, "Error starting game", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void leaveRoom() {
        if (isHost) {
            // Host leaving - delete the room
            roomRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Room closed", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                });
        } else {
            // Guest leaving - remove guest info
            Map<String, Object> updates = new HashMap<>();
            updates.put("guestId", "");
            updates.put("guestName", "");
            updates.put("guestReady", false);
            updates.put("status", "waiting");
            
            roomRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    finish();
                });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null && roomRef != null) {
            roomRef.removeEventListener(roomListener);
        }
    }
}
