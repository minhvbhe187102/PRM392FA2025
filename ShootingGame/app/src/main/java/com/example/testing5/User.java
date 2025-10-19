package com.example.testing5;

import java.util.List;

public class User {
    private String userId;
    private String username;
    private String email;
    private int currency;
    private List<String> unlockedSkins;
    private String selectedSkin;
    private int highScore;
    private int level;

    // Default constructor (required for Firestore)
    public User() {}

    // Constructor for new users
    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.currency = 100; // Starting currency
        this.unlockedSkins = new java.util.ArrayList<>();
        this.unlockedSkins.add("default_" + userId); // User's unique default skin
        this.selectedSkin = "default_" + userId;
        this.highScore = 0;
        this.level = 1;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCurrency() {
        return currency;
    }

    public void setCurrency(int currency) {
        this.currency = currency;
    }

    public List<String> getUnlockedSkins() {
        return unlockedSkins;
    }

    public void setUnlockedSkins(List<String> unlockedSkins) {
        this.unlockedSkins = unlockedSkins;
    }

    public String getSelectedSkin() {
        return selectedSkin;
    }

    public void setSelectedSkin(String selectedSkin) {
        this.selectedSkin = selectedSkin;
    }

    public int getHighScore() {
        return highScore;
    }

    public void setHighScore(int highScore) {
        this.highScore = highScore;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    // Helper methods
    public void addCurrency(int amount) {
        this.currency += amount;
    }

    public boolean spendCurrency(int amount) {
        if (this.currency >= amount) {
            this.currency -= amount;
            return true;
        }
        return false;
    }

    public void unlockSkin(String skinId) {
        if (!unlockedSkins.contains(skinId)) {
            unlockedSkins.add(skinId);
        }
    }

    public boolean hasSkin(String skinId) {
        return unlockedSkins.contains(skinId);
    }
}
