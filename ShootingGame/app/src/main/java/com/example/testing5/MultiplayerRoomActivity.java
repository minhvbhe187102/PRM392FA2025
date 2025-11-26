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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MultiplayerRoomActivity extends AppCompatActivity {
    
    private static final String TAG = "MultiplayerRoomActivity";
    
    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    
    private Button createRoomButton;
    private Button joinRoomButton;
    private Button backButton;
    private EditText roomCodeEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_room);
        
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
        database = FirebaseDatabase.getInstance("https://shootinggame-ff8aa-default-rtdb.asia-southeast1.firebasedatabase.app/");
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
        
        // Create room document in Realtime Database
        DatabaseReference roomRef = database.getReference("multiplayerRooms").child(roomCode);
        
        String hostName = currentUser.getDisplayName();
        if (hostName == null || hostName.isEmpty()) {
            hostName = currentUser.getEmail();
        }
        if (hostName == null || hostName.isEmpty()) {
            hostName = "Player";
        }
        Log.d(TAG, "Creating room - Host name: " + hostName + ", Email: " + currentUser.getEmail());
        
        Map<String, Object> room = new HashMap<>();
        room.put("roomCode", roomCode);
        room.put("hostId", currentUser.getUid());
        room.put("hostName", hostName);
        room.put("guestId", "");
        room.put("guestName", "");
        room.put("status", "waiting");
        room.put("createdAt", System.currentTimeMillis());
        room.put("expiresAt", System.currentTimeMillis() + (15 * 60 * 1000)); // 15 minutes
        room.put("hostReady", false);
        room.put("guestReady", false);
        room.put("gameStarted", false);
        
        roomRef.setValue(room)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(MultiplayerRoomActivity.this, "Room created! Code: " + roomCode, Toast.LENGTH_LONG).show();
                        
                        // Navigate to multiplayer lobby activity
                        Intent intent = new Intent(MultiplayerRoomActivity.this, MultiplayerLobbyActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("isHost", true);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Error creating room", task.getException());
                        Toast.makeText(MultiplayerRoomActivity.this, "Error creating room", Toast.LENGTH_SHORT).show();
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
        DatabaseReference roomRef = database.getReference("multiplayerRooms").child(roomCode);
        roomRef.get().addOnCompleteListener(new OnCompleteListener<com.google.firebase.database.DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<com.google.firebase.database.DataSnapshot> task) {
                if (task.isSuccessful()) {
                    com.google.firebase.database.DataSnapshot snapshot = task.getResult();
                    if (snapshot.exists()) {
                        String hostId = snapshot.child("hostId").getValue(String.class);
                        String guestId = snapshot.child("guestId").getValue(String.class);
                        String status = snapshot.child("status").getValue(String.class);
                        
                        // Check if room is available
                        if (hostId != null && (guestId == null || guestId.isEmpty()) && "waiting".equals(status)) {
                            // Check if user is not trying to join their own room
                            if (!hostId.equals(currentUser.getUid())) {
                                // Join the room
                                joinRoomInternal(roomCode);
                            } else {
                                Toast.makeText(MultiplayerRoomActivity.this, "Cannot join your own room", Toast.LENGTH_SHORT).show();
                            }
                        } else if (hostId != null && guestId != null && !guestId.isEmpty()) {
                            Toast.makeText(MultiplayerRoomActivity.this, "Room is full", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MultiplayerRoomActivity.this, "Room not available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MultiplayerRoomActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error checking room", task.getException());
                    Toast.makeText(MultiplayerRoomActivity.this, "Error checking room", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void joinRoomInternal(String roomCode) {
        // Update room with guest information
        DatabaseReference roomRef = database.getReference("multiplayerRooms").child(roomCode);
        
        String guestName = currentUser.getDisplayName();
        if (guestName == null || guestName.isEmpty()) {
            guestName = currentUser.getEmail();
        }
        if (guestName == null || guestName.isEmpty()) {
            guestName = "Player";
        }
        Log.d(TAG, "Joining room - Guest name: " + guestName + ", Email: " + currentUser.getEmail());
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("guestId", currentUser.getUid());
        updates.put("guestName", guestName);
        updates.put("status", "lobby");
        
        roomRef.updateChildren(updates)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(MultiplayerRoomActivity.this, "Joined room successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to multiplayer lobby activity
                        Intent intent = new Intent(MultiplayerRoomActivity.this, MultiplayerLobbyActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("isHost", false);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Error joining room", task.getException());
                        Toast.makeText(MultiplayerRoomActivity.this, "Error joining room", Toast.LENGTH_SHORT).show();
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
