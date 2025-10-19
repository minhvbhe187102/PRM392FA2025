package com.example.testing5;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FirebaseService() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    // Authentication methods
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public void signOut() {
        mAuth.signOut();
    }

    // User data methods
    public void createUserProfile(User user, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void getUserProfile(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void updateUserProfile(User user, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void updateUserCurrency(String userId, int newCurrency, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(userId)
                .update("currency", newCurrency)
                .addOnCompleteListener(listener);
    }

    public void updateUserHighScore(String userId, int newHighScore, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(userId)
                .update("highScore", newHighScore)
                .addOnCompleteListener(listener);
    }

    public void updateUserSelectedSkin(String userId, String skinId, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(userId)
                .update("selectedSkin", skinId)
                .addOnCompleteListener(listener);
    }

    public void unlockSkinForUser(String userId, String skinId, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                user.unlockSkin(skinId);
                                updateUserProfile(user, listener);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error unlocking skin", e);
                    }
                });
    }

    // Skin methods
    public void getAllSkins(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("skins")
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void getAvailableSkins(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("skins")
                .whereEqualTo("forSale", true)
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void getUniqueSkins(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("skins")
                .whereEqualTo("unique", true)
                .whereEqualTo("currentOwner", null) // Only unowned unique skins
                .get()
                .addOnCompleteListener(listener);
    }

    public void getSkinById(String skinId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("skins")
                .document(skinId)
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void createSkin(Skin skin, OnCompleteListener<Void> listener) {
        db.collection("skins")
                .document(skin.getSkinId())
                .set(skin)
                .addOnCompleteListener(listener);
    }
    
    public void purchaseSkin(String skinId, String userId, OnCompleteListener<Boolean> listener) {
        // Transaction to ensure atomic purchase
        db.runTransaction(transaction -> {
            // Get user document
            DocumentReference userRef = db.collection("users").document(userId);
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            User user = userSnapshot.toObject(User.class);
            
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            
            // Get skin document
            DocumentReference skinRef = db.collection("skins").document(skinId);
            DocumentSnapshot skinSnapshot = transaction.get(skinRef);
            Skin skin = skinSnapshot.toObject(Skin.class);
            
            if (skin == null) {
                throw new RuntimeException("Skin not found");
            }
            
            // Check if user has enough currency
            if (user.getCurrency() < skin.getPrice()) {
                throw new RuntimeException("Insufficient currency");
            }
            
            // Check if user is trying to buy their own skin
            if (skin.getCurrentOwner() != null && skin.getCurrentOwner().equals(userId)) {
                throw new RuntimeException("Cannot buy your own skin");
            }
            
            // Check if skin is actually for sale
            if (!skin.isForSale()) {
                throw new RuntimeException("Skin is not for sale");
            }
            
            // Store the previous owner before transferring ownership
            String previousOwnerId = skin.getCurrentOwner();
            
            // Read previous owner's document if there is one (must be done before any writes)
            User previousOwner = null;
            DocumentReference previousOwnerRef = null;
            if (previousOwnerId != null && !previousOwnerId.equals(userId)) {
                previousOwnerRef = db.collection("users").document(previousOwnerId);
                DocumentSnapshot previousOwnerDoc = transaction.get(previousOwnerRef);
                if (previousOwnerDoc.exists()) {
                    previousOwner = previousOwnerDoc.toObject(User.class);
                }
            }
            
            // Deduct currency from buyer
            user.setCurrency(user.getCurrency() - skin.getPrice());
            user.unlockSkin(skinId);
            
            // Update buyer's profile
            transaction.set(userRef, user);
            
            // Transfer skin ownership
            skin.setCurrentOwner(userId);
            skin.setForSale(false); // Remove from sale
            transaction.set(skinRef, skin);
            
            // If there was a previous owner, give them the currency
            if (previousOwner != null && previousOwnerRef != null) {
                previousOwner.setCurrency(previousOwner.getCurrency() + skin.getPrice());
                transaction.set(previousOwnerRef, previousOwner);
            }
            
            return true;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                        listener.onComplete(new Task<Boolean>() {
                            @Override
                            public boolean isComplete() { return true; }
                            @Override
                            public boolean isSuccessful() { return true; }
                            @Override
                            public boolean isCanceled() { return false; }
                            @Override
                            public Boolean getResult() { return true; }
                            @Override
                            public <X extends Throwable> Boolean getResult(Class<X> aClass) throws X { return true; }
                            @Override
                            public Exception getException() { return null; }
                            @Override
                            public Task<Boolean> addOnCompleteListener(OnCompleteListener<Boolean> onCompleteListener) {
                                onCompleteListener.onComplete(this);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnCompleteListener(Executor executor, OnCompleteListener<Boolean> onCompleteListener) {
                                onCompleteListener.onComplete(this);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnCompleteListener(android.app.Activity activity, OnCompleteListener<Boolean> onCompleteListener) {
                                onCompleteListener.onComplete(this);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnSuccessListener(OnSuccessListener<? super Boolean> onSuccessListener) {
                                onSuccessListener.onSuccess(true);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnSuccessListener(Executor executor, OnSuccessListener<? super Boolean> onSuccessListener) {
                                onSuccessListener.onSuccess(true);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnSuccessListener(android.app.Activity activity, OnSuccessListener<? super Boolean> onSuccessListener) {
                                onSuccessListener.onSuccess(true);
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnFailureListener(OnFailureListener onFailureListener) {
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnFailureListener(Executor executor, OnFailureListener onFailureListener) {
                                return this;
                            }
                            @Override
                            public Task<Boolean> addOnFailureListener(android.app.Activity activity, OnFailureListener onFailureListener) {
                                return this;
                            }
                        });
            } else {
                listener.onComplete(new Task<Boolean>() {
                    @Override
                    public boolean isComplete() { return true; }
                    @Override
                    public boolean isSuccessful() { return false; }
                    @Override
                    public boolean isCanceled() { return false; }
                    @Override
                    public Boolean getResult() { return false; }
                    @Override
                    public <X extends Throwable> Boolean getResult(Class<X> aClass) throws X { 
                        if (task.getException() != null && aClass.isInstance(task.getException())) {
                            throw aClass.cast(task.getException());
                        }
                        return false; 
                    }
                    @Override
                    public Exception getException() { return task.getException(); }
                    @Override
                    public Task<Boolean> addOnCompleteListener(OnCompleteListener<Boolean> onCompleteListener) {
                        onCompleteListener.onComplete(this);
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnCompleteListener(Executor executor, OnCompleteListener<Boolean> onCompleteListener) {
                        onCompleteListener.onComplete(this);
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnCompleteListener(android.app.Activity activity, OnCompleteListener<Boolean> onCompleteListener) {
                        onCompleteListener.onComplete(this);
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnSuccessListener(OnSuccessListener<? super Boolean> onSuccessListener) {
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnSuccessListener(Executor executor, OnSuccessListener<? super Boolean> onSuccessListener) {
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnSuccessListener(android.app.Activity activity, OnSuccessListener<? super Boolean> onSuccessListener) {
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnFailureListener(OnFailureListener onFailureListener) {
                        onFailureListener.onFailure(task.getException());
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnFailureListener(Executor executor, OnFailureListener onFailureListener) {
                        onFailureListener.onFailure(task.getException());
                        return this;
                    }
                    @Override
                    public Task<Boolean> addOnFailureListener(android.app.Activity activity, OnFailureListener onFailureListener) {
                        onFailureListener.onFailure(task.getException());
                        return this;
                    }
                });
            }
        }); 
    }
    
    public void getUserOwnedSkins(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("skins")
                .whereEqualTo("currentOwner", userId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Helper method to convert DocumentSnapshot to User
    public User documentToUser(DocumentSnapshot document) {
        if (document.exists()) {
            return document.toObject(User.class);
        }
        return null;
    }

    // Helper method to convert DocumentSnapshot to Skin
    public Skin documentToSkin(DocumentSnapshot document) {
        if (document.exists()) {
            return document.toObject(Skin.class);
        }
        return null;
    }
    
    // Shop-related methods
    
    /**
     * Get skins that are for sale (not owned by the current user)
     */
    public void getSkinsForSale(String currentUserId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("skins")
            .whereEqualTo("forSale", true)
            .get()
            .addOnCompleteListener(listener);
    }
    
    /**
     * Update a skin (for putting it up for sale)
     */
    public void updateSkin(Skin skin, OnCompleteListener<Void> listener) {
        db.collection("skins").document(skin.getSkinId())
            .set(skin)
            .addOnCompleteListener(listener);
    }
}
