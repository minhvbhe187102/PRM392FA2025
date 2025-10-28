package com.example.testing5;

public class Skin {
    
    // Rarity enum
    public enum Rarity {
        COMMON("Common"),
        UNCOMMON("Uncommon"), 
        RARE("Rare");
        
        private final String displayName;
        
        Rarity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    private String skinId;
    private String name;
    private String description;
    private int price;
    private String imageBase64; // Base64 encoded image
    private String currentOwner; // User ID of current owner (null if available)
    private String creatorId; // User ID of creator
    private long createdAt; // Timestamp
    private boolean forSale; // Whether skin is available for purchase
    private boolean unique; // True for unique/NFT-style skins
    private Rarity rarity; // Skin rarity (COMMON, UNCOMMON, RARE)

    // Default constructor (required for Firestore)
    public Skin() {}

    // Constructor for unique skins
    public Skin(String skinId, String name, String description, int price, String imageBase64, 
                String creatorId, boolean isUnique, Rarity rarity) {
        this.skinId = skinId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageBase64 = imageBase64;
        this.currentOwner = null; // No owner initially
        this.creatorId = creatorId;
        this.createdAt = System.currentTimeMillis();
        this.forSale = true;
        this.unique = isUnique;
        this.rarity = rarity;
    }
    
    // Constructor for non-unique skins (unlimited copies)
    public Skin(String skinId, String name, String description, int price, String imageBase64, Rarity rarity) {
        this.skinId = skinId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageBase64 = imageBase64;
        this.currentOwner = null;
        this.creatorId = "system";
        this.createdAt = System.currentTimeMillis();
        this.forSale = true;
        this.unique = false;
        this.rarity = rarity;
    }

    // Getters and Setters
    public String getSkinId() {
        return skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public void setCurrentOwner(String currentOwner) {
        this.currentOwner = currentOwner;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isForSale() {
        return forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }

    // Helper methods
    public boolean isOwnedBy(String userId) {
        return currentOwner != null && currentOwner.equals(userId);
    }
}
