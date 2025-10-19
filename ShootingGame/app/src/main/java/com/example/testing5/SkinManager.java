package com.example.testing5;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class SkinManager {
    private static SkinManager instance;

    private SkinManager() {}

    public static synchronized SkinManager getInstance() {
        if (instance == null) {
            instance = new SkinManager();
        }
        return instance;
    }

    // Convert Bitmap to Base64 string
    public String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Convert Base64 string to Bitmap
    public Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;
        
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Set skin image from base64 to ImageView
    public void setSkinToImageView(ImageView imageView, String base64Image) {
        if (imageView == null || base64Image == null) return;
        
        Bitmap bitmap = base64ToBitmap(base64Image);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
    
    // Alias method for compatibility
    public void setImageFromBase64(ImageView imageView, String base64Image) {
        setSkinToImageView(imageView, base64Image);
    }

    // Generate unique skin ID
    public String generateSkinId() {
        return "skin_" + UUID.randomUUID().toString();
    }

    // Create a default skin with simple color
    public Skin createDefaultSkin() {
        // Create a simple 64x64 colored bitmap
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF00FF00); // Green color
        
        String base64 = bitmapToBase64(bitmap);
        return new Skin("default", "Default Skin", "Free starter skin", 0, base64);
    }

    // Create example unique skin
    public Skin createExampleUniqueSkin(String creatorId, int price) {
        // Create a simple 64x64 colored bitmap with random color
        int red = (int) (Math.random() * 256);
        int green = (int) (Math.random() * 256);
        int blue = (int) (Math.random() * 256);
        int color = 0xFF000000 | (red << 16) | (green << 8) | blue;
        
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        
        String base64 = bitmapToBase64(bitmap);
        String skinId = generateSkinId();
        String name = "Unique Skin #" + skinId.substring(5, 10);
        
        return new Skin(skinId, name, "One-of-a-kind skin", price, base64, creatorId, true);
    }

    // Compress base64 if too large
    public String compressBase64Image(String base64, int maxSizeKB) {
        Bitmap bitmap = base64ToBitmap(base64);
        if (bitmap == null) return base64;
        
        int quality = 100;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        do {
            outputStream.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            quality -= 10;
        } while (outputStream.size() / 1024 > maxSizeKB && quality > 0);
        
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}
