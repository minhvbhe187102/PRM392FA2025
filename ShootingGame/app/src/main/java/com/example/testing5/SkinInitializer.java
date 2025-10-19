package com.example.testing5;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Utility class to initialize example skins in Firebase
 * Run this once to populate your database with starter skins
 */
public class SkinInitializer {
    private static final String TAG = "SkinInitializer";
    private FirebaseService firebaseService;
    private SkinManager skinManager;

    public SkinInitializer() {
        firebaseService = FirebaseService.getInstance();
        skinManager = SkinManager.getInstance();
    }

    // Call this method to create example skins in Firebase
    public void initializeExampleSkins() {
        createDefaultSkin();
        createUniqueSkin1();
        createUniqueSkin2();
        createUniqueSkin3();
    }

    private void createDefaultSkin() {
        // Green default skin - free and always available
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF00FF00); // Green
        String base64 = skinManager.bitmapToBase64(bitmap);
        
        Skin skin = new Skin("default", "Default Skin", "Free starter skin", 0, base64);
        
        firebaseService.createSkin(skin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Default skin created");
                } else {
                    Log.e(TAG, "Error creating default skin", task.getException());
                }
            }
        });
    }

    private void createUniqueSkin1() {
        // Red unique skin
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFFF0000); // Red
        String base64 = skinManager.bitmapToBase64(bitmap);
        
        Skin skin = new Skin(skinManager.generateSkinId(), "Fire Skin", 
                "Unique red blazing skin", 500, base64, "system", true);
        
        firebaseService.createSkin(skin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Unique Fire skin created");
                } else {
                    Log.e(TAG, "Error creating Fire skin", task.getException());
                }
            }
        });
    }

    private void createUniqueSkin2() {
        // Blue unique skin
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF0000FF); // Blue
        String base64 = skinManager.bitmapToBase64(bitmap);
        
        Skin skin = new Skin(skinManager.generateSkinId(), "Ice Skin", 
                "Unique blue frozen skin", 750, base64, "system", true);
        
        firebaseService.createSkin(skin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Unique Ice skin created");
                } else {
                    Log.e(TAG, "Error creating Ice skin", task.getException());
                }
            }
        });
    }

    private void createUniqueSkin3() {
        // Gold unique skin
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFFFD700); // Gold
        String base64 = skinManager.bitmapToBase64(bitmap);
        
        Skin skin = new Skin(skinManager.generateSkinId(), "Gold Skin", 
                "Unique golden legendary skin", 1000, base64, "system", true);
        
        firebaseService.createSkin(skin, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Unique Gold skin created");
                } else {
                    Log.e(TAG, "Error creating Gold skin", task.getException());
                }
            }
        });
    }

    // Method to create a custom unique skin from base64
    public void createCustomUniqueSkin(String name, String description, int price, 
                                      String imageBase64, String creatorId, 
                                      OnCompleteListener<Void> listener) {
        String skinId = skinManager.generateSkinId();
        Skin skin = new Skin(skinId, name, description, price, imageBase64, creatorId, true);
        
        firebaseService.createSkin(skin, listener);
    }
}
