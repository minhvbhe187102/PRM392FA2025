package com.example.testing5;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseTestActivity extends AppCompatActivity {
    
    private static final String TAG = "FirebaseTestActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Test Firebase Realtime Database connection
        testRealtimeDatabase();
    }
    
    private void testRealtimeDatabase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://shootinggame-ff8aa-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference testRef = database.getReference("test");
            
            // Try to write a test value
            testRef.setValue("Hello Firebase!")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Firebase Realtime Database write successful!");
                        Toast.makeText(this, "✅ Realtime Database working!", Toast.LENGTH_LONG).show();
                        
                        // Try to read it back
                        testRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String value = snapshot.getValue(String.class);
                                Log.d(TAG, "✅ Firebase Realtime Database read successful: " + value);
                                Toast.makeText(FirebaseTestActivity.this, "✅ Read successful: " + value, Toast.LENGTH_LONG).show();
                                
                                // Clean up test data
                                testRef.removeValue();
                                finish();
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Firebase Realtime Database read failed", error.toException());
                                Toast.makeText(FirebaseTestActivity.this, "❌ Read failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    } else {
                        Log.e(TAG, "❌ Firebase Realtime Database write failed", task.getException());
                        Toast.makeText(this, "❌ Write failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
                
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase Realtime Database initialization failed", e);
            Toast.makeText(this, "❌ Initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
