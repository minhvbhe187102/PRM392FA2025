package com.example.testing5;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class MultiplayerGameActivity extends AppCompatActivity {
    
    private static final String TAG = "MultiplayerGameActivity";
    
    // Firebase
    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference roomRef;
    private DatabaseReference gameStateRef;
    private DatabaseReference inputRef;
    private ValueEventListener gameStateListener;
    
    // Room info
    private String roomCode;
    private boolean isHost;
    private String playerId;
    private String otherPlayerId;
    
    // Game state
    private boolean gameOver = false;
    private int currentLevel = 1;
    private int currentExp = 0;
    private int expToNextLevel = 10;
    private int[] contentWidth = {0};
    private int[] contentHeight = {0};
    
    // UI elements
    private ConstraintLayout mainLayout;
    private ImageView leftBigCircle, leftSmallCircle;
    private ImageView playerBigCircle, playerSmallCircle;
    private ImageView rightBigCircle, rightSmallCircle;
    private ImageView otherPlayerBigCircle, otherPlayerSmallCircle;
    private TextView levelText;
    private View expBarProgress;
    
    // Game objects
    private ArrayList<ImageView> activeProjectiles = new ArrayList<>();
    private ArrayList<ImageView> activeEnemies = new ArrayList<>();
    private Handler gameHandler = new Handler();
    
    // Entity tracking maps for guest (key: entity ID, value: ImageView)
    private HashMap<String, ImageView> syncedEnemies = new HashMap<>();
    private HashMap<String, ImageView> syncedProjectiles = new HashMap<>();
    
    // Track last known positions to prevent unnecessary updates (key: entity ID, value: position)
    private HashMap<String, float[]> lastEnemyPositions = new HashMap<>(); // [x, y]
    private HashMap<String, float[]> lastProjectilePositions = new HashMap<>(); // [x, y]
    
    // Track missing frames to prevent premature removal (key: entity ID, value: consecutive missing frames)
    private HashMap<String, Integer> enemyMissingFrames = new HashMap<>();
    private static final int MISSING_FRAMES_THRESHOLD = 5; // Remove enemy after missing for 5 consecutive frames
    
    // Unique ID counters for host
    private int enemyIdCounter = 0;
    private int projectileIdCounter = 0;
    
    // Maps for host to track entity IDs (ImageView -> ID)
    private HashMap<ImageView, String> enemyIdMap = new HashMap<>();
    private HashMap<ImageView, String> projectileIdMap = new HashMap<>();
    
    // Control angles
    private double leftAngle = 0;        // Movement direction
    private double rightAngle = 0;      // Shooting direction
    private double otherPlayerAngle = 0; // Other player's shooting direction
    private boolean isShooting = false;
    private boolean otherPlayerShooting = false;
    
    // Player position
    private float playerX = 0;
    private float playerY = 0;
    private float otherPlayerX = 0;
    private float otherPlayerY = 0;
    private boolean otherPlayerPositionReceived = false; // Track if we've received first position update
    private long lastVisualUpdateTime = 0; // Throttle visual updates to prevent flickering
    private static final long MIN_VISUAL_UPDATE_INTERVAL_MS = 50; // Minimum 50ms between visual updates
    
    // Circle pairs for control
    class CirclePair {
        ImageView big;
        ImageView small;
        double currentAngle;
        boolean isMoving;
        int pointerId = -1;
        boolean fingerin;

        CirclePair(ImageView big, ImageView small) {
            this.big = big;
            this.small = small;
            this.currentAngle = 0;
            this.isMoving = false;
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_multiplayer_game);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });
        
        // Get room info from intent
        roomCode = getIntent().getStringExtra("roomCode");
        isHost = getIntent().getBooleanExtra("isHost", false);
        
        initializeViews();
        initializeFirebase();
        setupGameStateListener();
        
        if (!isHost) {
            startGuestGame();
        }
    }
    
    private void initializeViews() {
        mainLayout = findViewById(R.id.main);
        leftBigCircle = findViewById(R.id.leftBigCircle);
        leftSmallCircle = findViewById(R.id.leftSmallCircle);
        playerBigCircle = findViewById(R.id.playerBigCircle);
        playerSmallCircle = findViewById(R.id.playerSmallCircle);
        rightBigCircle = findViewById(R.id.rightBigCircle);
        rightSmallCircle = findViewById(R.id.rightSmallCircle);
        otherPlayerBigCircle = findViewById(R.id.otherPlayerBigCircle);
        otherPlayerSmallCircle = findViewById(R.id.otherPlayerSmallCircle);
        levelText = findViewById(R.id.levelText);
        expBarProgress = findViewById(R.id.expBarProgress);
        
        // Set up touch handling
        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouch(event);
            }
        });
        
        // Get content dimensions after layout
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                contentWidth[0] = mainLayout.getWidth();
                contentHeight[0] = mainLayout.getHeight();
                
                // Initialize player position to center
                playerX = contentWidth[0] / 2f;
                playerY = contentHeight[0] / 2f;
                
                mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
    
    private void initializeFirebase() {
        database = FirebaseDatabase.getInstance("https://shootinggame-ff8aa-default-rtdb.asia-southeast1.firebasedatabase.app/");
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Log.e(TAG, "User not logged in");
            finish();
            return;
        }
        
        playerId = currentUser.getUid();
        roomRef = database.getReference("multiplayerRooms").child(roomCode);
        gameStateRef = database.getReference("gameState").child(roomCode);
        inputRef = database.getReference("gameInput").child(roomCode);
        
        // Get the other player's ID
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (isHost) {
                        otherPlayerId = snapshot.child("guestId").getValue(String.class);
                    } else {
                        otherPlayerId = snapshot.child("hostId").getValue(String.class);
                    }
                    Log.d(TAG, "Other player ID: " + otherPlayerId);
                    
                    // Start input reading loop only after we have the other player's ID
                    if (otherPlayerId != null) {
                        startInputReadingLoop();
                        
                        // For host, also start the game loops
                        if (isHost) {
                            startHostGame();
                        }
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get other player ID", error.toException());
            }
        });
    }
    
    private void setupGameStateListener() {
        gameStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                
                updateGameState(snapshot);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Game state listener cancelled", error.toException());
            }
        };
        
        gameStateRef.addValueEventListener(gameStateListener);
    }
    
    private void updateGameState(DataSnapshot snapshot) {
        if (gameOver) return;
        
        Log.d(TAG, (isHost ? "Host" : "Guest") + ": updateGameState called");
        
        // Update level and exp
        Integer level = snapshot.child("level").getValue(Integer.class);
        Integer exp = snapshot.child("exp").getValue(Integer.class);
        Boolean gameOverState = snapshot.child("gameOver").getValue(Boolean.class);
        
        if (level != null) {
            currentLevel = level;
            levelText.setText("Level " + currentLevel);
        }
        
        if (exp != null) {
            currentExp = exp;
            updateExpBar();
        }
        
        if (gameOverState != null && gameOverState) {
            endGame();
        }
        
        // Update other player's position and shooting direction
        boolean positionUpdated = false;
        boolean angleUpdated = false;
        
        if (isHost) {
            // Host receives guest data
            Double guestAngle = snapshot.child("guestPlayerAngle").getValue(Double.class);
            Float guestX = snapshot.child("guestPlayerX").getValue(Float.class);
            Float guestY = snapshot.child("guestPlayerY").getValue(Float.class);
            Boolean guestShooting = snapshot.child("guestPlayerShooting").getValue(Boolean.class);
            
            // Only update angle if we have a valid value
            if (guestAngle != null && otherPlayerAngle != guestAngle) {
                otherPlayerAngle = guestAngle;
                angleUpdated = true;
            }
            
            // Only update position if we have valid values
            if (guestX != null && guestY != null) {
                // STRICT validation: Reject (0,0) completely - it's always invalid initial state
                boolean isWithinBounds = isValidPosition(guestX, guestY);
                boolean isNotZero = (guestX != 0f && guestY != 0f);
                
                // Check if position jump is too large (indicates invalid/corrupted data)
                boolean reasonableJump = true;
                if (otherPlayerPositionReceived) {
                    float dx = guestX - otherPlayerX;
                    float dy = guestY - otherPlayerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    // Reject if position jumps more than 500 pixels (impossible for normal movement)
                    reasonableJump = distance <= 500f;
                }
                
                // Only accept position if: within bounds AND not (0,0) AND reasonable jump
                if (isWithinBounds && isNotZero && reasonableJump) {
                    // Only update if values actually changed to avoid unnecessary updates
                    if (!otherPlayerPositionReceived || (otherPlayerX != guestX || otherPlayerY != guestY)) {
                        otherPlayerX = guestX;
                        otherPlayerY = guestY;
                        otherPlayerPositionReceived = true;
                        positionUpdated = true;
                    }
                }
            }
            
            // Only update shooting state if we have a valid value
            if (guestShooting != null) {
                otherPlayerShooting = guestShooting;
            }
        } else {
            // Guest receives host data
            Double hostAngle = snapshot.child("hostPlayerAngle").getValue(Double.class);
            Float hostX = snapshot.child("hostPlayerX").getValue(Float.class);
            Float hostY = snapshot.child("hostPlayerY").getValue(Float.class);
            Boolean hostShooting = snapshot.child("hostPlayerShooting").getValue(Boolean.class);
            
            // Only update angle if we have a valid value
            if (hostAngle != null && otherPlayerAngle != hostAngle) {
                otherPlayerAngle = hostAngle;
                angleUpdated = true;
            }
            
            // Only update position if we have valid values
            if (hostX != null && hostY != null) {
                // STRICT validation: Reject (0,0) completely - it's always invalid initial state
                boolean isWithinBounds = isValidPosition(hostX, hostY);
                boolean isNotZero = (hostX != 0f && hostY != 0f);
                
                // Check if position jump is too large (indicates invalid/corrupted data)
                boolean reasonableJump = true;
                if (otherPlayerPositionReceived) {
                    float dx = hostX - otherPlayerX;
                    float dy = hostY - otherPlayerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    // Reject if position jumps more than 500 pixels (impossible for normal movement)
                    reasonableJump = distance <= 10f;
                }
                
                // Only accept position if: within bounds AND not (0,0) AND reasonable jump
                if (isWithinBounds && isNotZero && reasonableJump) {
                    // Only update if values actually changed to avoid unnecessary updates
                    if (!otherPlayerPositionReceived || (otherPlayerX != hostX || otherPlayerY != hostY)) {
                        otherPlayerX = hostX;
                        otherPlayerY = hostY;
                        otherPlayerPositionReceived = true;
                        positionUpdated = true;
                    }
                }
            }
            
            // Only update shooting state if we have a valid value
            if (hostShooting != null) {
                otherPlayerShooting = hostShooting;
            }
        }
        
        // Only update visual position if we received a valid position or angle update
        // This prevents flickering from invalid or stale position data
        if (positionUpdated || (angleUpdated && otherPlayerPositionReceived)) {
            // Throttle visual updates to prevent flickering from rapid Firebase updates
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVisualUpdateTime >= MIN_VISUAL_UPDATE_INTERVAL_MS) {
                updateOtherPlayerPosition();
                lastVisualUpdateTime = currentTime;
            }
        }
        
        // Update enemies and projectiles (host only updates, guests receive)
        if (!isHost) {
            Log.d(TAG, "Guest: About to update enemies and projectiles from snapshot");
            updateEnemiesFromSnapshot(snapshot);
            updateProjectilesFromSnapshot(snapshot);
        } else {
            Log.d(TAG, "Host: Skipping enemy/projectile sync (host manages them locally)");
        }
    }
    
    private boolean isValidPosition(float x, float y) {
        // Screen hasn't been measured yet
        if (contentWidth[0] <= 0 || contentHeight[0] <= 0) {
            return false;
        }
        
        // Position must be within reasonable bounds (with some margin for off-screen entities)
        // Allow positions slightly outside screen bounds (e.g., -100 to width+100)
        float margin = 200f;
        return x >= -margin && x <= contentWidth[0] + margin &&
               y >= -margin && y <= contentHeight[0] + margin;
    }
    
    private void updateOtherPlayerPosition() {
        // Only show other player if we've received their position
        if (!otherPlayerPositionReceived) {
            if (otherPlayerBigCircle != null) {
                otherPlayerBigCircle.setVisibility(View.INVISIBLE);
            }
            if (otherPlayerSmallCircle != null) {
                otherPlayerSmallCircle.setVisibility(View.INVISIBLE);
            }
            return;
        }
        
        // Position the other player's big circle at their synced X/Y
        if (otherPlayerBigCircle != null) {
            float circleX = otherPlayerX - otherPlayerBigCircle.getWidth() / 2f;
            float circleY = otherPlayerY - otherPlayerBigCircle.getHeight() / 2f;
            otherPlayerBigCircle.setVisibility(View.VISIBLE);
            otherPlayerBigCircle.setX(circleX);
            otherPlayerBigCircle.setY(circleY);
        }

        // Place the other player's small circle along their aim angle
        if (otherPlayerSmallCircle != null) {
            otherPlayerSmallCircle.setVisibility(View.VISIBLE);
            float radius = 30f;
            float smallX = otherPlayerX + (float) (Math.cos(otherPlayerAngle) * radius) - otherPlayerSmallCircle.getWidth() / 2f;
            float smallY = otherPlayerY + (float) (Math.sin(otherPlayerAngle) * radius) - otherPlayerSmallCircle.getHeight() / 2f;
            otherPlayerSmallCircle.setX(smallX);
            otherPlayerSmallCircle.setY(smallY);
        }
    }
    
    private void startHostGame() {
        // Host runs the authoritative game loop
        initializeGameState();
        startEnemySpawnLoop();
        startShootingLoop();
        startProjectileUpdateLoop();
        startInputReadingLoop();
        startPlayerMovementLoop();
    }
    
    private void startGuestGame() {
        // Guest sends input and receives state updates, also updates their position in gameState
        startInputSendingLoop();
        startPlayerMovementLoop();
        startGuestStateUpdateLoop(); // Guest needs to update their position in gameState
    }
    
    private void startGuestStateUpdateLoop() {
        // Guest updates their position in gameState so host can see them
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                updateGameStateInFirebase();
                gameHandler.postDelayed(this, 100); // Update every 100ms
            }
        };
        
        gameHandler.post(updateRunnable);
    }
    
    private void initializeGameState() {
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("level", currentLevel);
        gameState.put("exp", currentExp);
        gameState.put("gameOver", false);
        
        // DO NOT write host position here - it's likely (0,0) before layout is measured
        // Position will be set by updateGameStateInFirebase() once layout is measured and playerX/Y are valid
        // This prevents guest from seeing ghost at (0,0)
        gameState.put("hostPlayerAngle", rightAngle);
        gameState.put("hostPlayerShooting", isShooting);
        
        // Don't initialize guest position - let guest set it when they join
        // This prevents the "ghost" player at (0,0) from appearing
        gameState.put("enemies", new HashMap<String, Object>());
        gameState.put("projectiles", new HashMap<String, Object>());
        
        // Clear any old data and set fresh game state
        gameStateRef.setValue(gameState);
    }
    
    private void startPlayerMovementLoop() {
        Runnable movementRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                // Move player based on left angle
                float speed = 200f; // pixels per second
                float dx = (float) (Math.cos(leftAngle) * speed * 0.016f); // 16ms frame
                float dy = (float) (Math.sin(leftAngle) * speed * 0.016f);
                
                playerX += dx;
                playerY += dy;
                
                // Keep player within bounds
                playerX = Math.max(50f, Math.min(contentWidth[0] - 50f, playerX));
                playerY = Math.max(50f, Math.min(contentHeight[0] - 50f, playerY));
                
                // Update player position
                playerBigCircle.setX(playerX - playerBigCircle.getWidth() / 2f);
                playerBigCircle.setY(playerY - playerBigCircle.getHeight() / 2f);
                
                // Update player shooting direction (right angle controls player small circle)
                updatePlayerShootingDirection();
                
                gameHandler.postDelayed(this, 16);
            }
        };
        
        gameHandler.post(movementRunnable);
    }
    
    private void updatePlayerShootingDirection() {
        float radius = 30f;
        float smallX = playerX + (float) (Math.cos(rightAngle) * radius) - playerSmallCircle.getWidth() / 2f;
        float smallY = playerY + (float) (Math.sin(rightAngle) * radius) - playerSmallCircle.getHeight() / 2f;
        
        playerSmallCircle.setX(smallX);
        playerSmallCircle.setY(smallY);
    }
    
    private void startEnemySpawnLoop() {
        Runnable enemySpawnRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                spawnEnemy();
                
                // Spawn interval decreases over time (5s to 1s)
                int interval = Math.max(1000, 5000 - (currentLevel * 200));
                gameHandler.postDelayed(this, interval);
            }
        };
        
        gameHandler.postDelayed(enemySpawnRunnable, 5000);
    }
    
    private void spawnEnemy() {
        if (contentWidth[0] <= 0 || contentHeight[0] <= 0) return;
        
        enemy e = enemy.randomExample();
        if (e == null) {
            e = new enemy(0, "fallback", 1, 10, 10);
        }
        
        float density = getResources().getDisplayMetrics().density;
        int enemySizePx = (int) (e.getSize() * 2.4f * density);
        
        ImageView enemy = new ImageView(this);
        enemy.setLayoutParams(new ConstraintLayout.LayoutParams(enemySizePx, enemySizePx));
        enemy.setImageResource(R.drawable.enemy_square);
        
        // Spawn randomly outside screen edges
        int edge = (int) (Math.random() * 4);
        float spawnX, spawnY;
        switch (edge) {
            case 0: // left
                spawnX = -enemySizePx - 10;
                spawnY = (float) (Math.random() * (contentHeight[0] - enemySizePx));
                break;
            case 1: // top
                spawnX = (float) (Math.random() * (contentWidth[0] - enemySizePx));
                spawnY = -enemySizePx - 10;
                break;
            case 2: // right
                spawnX = contentWidth[0] + 10;
                spawnY = (float) (Math.random() * (contentHeight[0] - enemySizePx));
                break;
            default: // bottom
                spawnX = (float) (Math.random() * (contentWidth[0] - enemySizePx));
                spawnY = contentHeight[0] + 10;
                break;
        }
        
        enemy.setX(spawnX);
        enemy.setY(spawnY);
        
        // Assign unique ID to enemy
        String enemyId = "enemy_" + enemyIdCounter++;
        enemyIdMap.put(enemy, enemyId);
        enemy.setTag(enemyId);
        
        mainLayout.addView(enemy);
        activeEnemies.add(enemy);
        
        // Move enemy towards center
        moveEnemyTowardsCenter(enemy);
    }
    
    private void moveEnemyTowardsCenter(ImageView enemy) {
        Runnable moveRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver || enemy.getParent() == null) return;
                
                float centerX = contentWidth[0] / 2f;
                float centerY = contentHeight[0] / 2f;
                float enemyX = enemy.getX() + enemy.getWidth() / 2f;
                float enemyY = enemy.getY() + enemy.getHeight() / 2f;
                
                float dx = centerX - enemyX;
                float dy = centerY - enemyY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (distance > 0) {
                    float speed = 100f; // pixels per second
                    float moveX = (dx / distance) * speed * 0.016f; // 16ms frame
                    float moveY = (dy / distance) * speed * 0.016f;
                    
                    enemy.setX(enemy.getX() + moveX);
                    enemy.setY(enemy.getY() + moveY);
                    
                    // Check collision with players
                    if (viewsIntersect(enemy, playerBigCircle)) {
                        //endGame();
                        return;
                    }
                    
                    // Check collision with other player
                    if (otherPlayerBigCircle != null && viewsIntersect(enemy, otherPlayerBigCircle)) {
                        //  endGame();
                        return;
                    }
                    
                    // Check collision with projectiles
                    for (ImageView projectile : activeProjectiles) {
                        if (viewsIntersect(enemy, projectile)) {
                            // Enemy hit - remove both
                            mainLayout.removeView(enemy);
                            mainLayout.removeView(projectile);
                            activeEnemies.remove(enemy);
                            activeProjectiles.remove(projectile);
                            
                            // Clean up ID maps
                            enemyIdMap.remove(enemy);
                            projectileIdMap.remove(projectile);
                            
                            // Gain experience
                            gainExperience();
                            return;
                        }
                    }
                }
                
                gameHandler.postDelayed(this, 16);
            }
        };
        
        gameHandler.post(moveRunnable);
    }
    
    private void startShootingLoop() {
        Runnable shootRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                if (isHost) {
                    // Host handles shooting for both players
                    if (isShooting) {
                        shootProjectile(playerX, playerY, rightAngle, true); // Host's projectile
                    }
                    if (otherPlayerShooting) {
                        shootProjectile(otherPlayerX, otherPlayerY, otherPlayerAngle, false); // Guest's projectile
                    }
                } else {
                    // Guest only shoots locally (host will handle the actual projectile)
                    if (isShooting) {
                        shootProjectile(playerX, playerY, rightAngle, true); // Guest's projectile
                    }
                }
                
                gameHandler.postDelayed(this, 1000); // Shoot every second
            }
        };
        
        gameHandler.post(shootRunnable);
    }
    
    private void shootProjectile(float x, float y, double angle, boolean isHostProjectile) {
        int projectileSize = 32;
        ImageView projectile = new ImageView(this);
        projectile.setLayoutParams(new ConstraintLayout.LayoutParams(projectileSize, projectileSize));
        projectile.setImageResource(R.drawable.circle);
        
        float startX = x - projectileSize / 2f;
        float startY = y - projectileSize / 2f;
        
        projectile.setX(startX);
        projectile.setY(startY);
        
        // Assign unique ID to projectile (only for host)
        if (isHost) {
            String projectileId = "projectile_" + projectileIdCounter++;
            projectileIdMap.put(projectile, projectileId);
            projectile.setTag(projectileId);
        }
        
        mainLayout.addView(projectile);
        activeProjectiles.add(projectile);
        
        // Move projectile
        moveProjectile(projectile, angle);
    }
    
    private void moveProjectile(ImageView projectile, double angle) {
        Runnable moveRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver || projectile.getParent() == null) return;
                
                float speedPxPerSecond = 600f;
                float dx = (float) (Math.cos(angle) * speedPxPerSecond * 0.016f);
                float dy = (float) (Math.sin(angle) * speedPxPerSecond * 0.016f);
                
                projectile.setX(projectile.getX() + dx);
                projectile.setY(projectile.getY() + dy);
                
                // Remove if out of bounds
                float x = projectile.getX();
                float y = projectile.getY();
                if (x < -projectile.getWidth() || x > contentWidth[0] || 
                    y < -projectile.getHeight() || y > contentHeight[0]) {
                    mainLayout.removeView(projectile);
                    activeProjectiles.remove(projectile);
                    projectileIdMap.remove(projectile);
                    return;
                }
                
                gameHandler.postDelayed(this, 16);
            }
        };
        
        gameHandler.post(moveRunnable);
    }
    
    private void startProjectileUpdateLoop() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                updateGameStateInFirebase();
                updateEnemiesAndProjectilesInFirebase();
                gameHandler.postDelayed(this, 100); // Update every 100ms
            }
        };
        
        gameHandler.post(updateRunnable);
    }
    
    private void startInputReadingLoop() {
        Runnable inputRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                readInputFromFirebase();
                gameHandler.postDelayed(this, 50); // Read input every 50ms
            }
        };
        
        gameHandler.post(inputRunnable);
    }
    
    private void startInputSendingLoop() {
        Runnable inputRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver) return;
                
                sendInputToFirebase();
                gameHandler.postDelayed(this, 50); // Send input every 50ms
            }
        };
        
        gameHandler.post(inputRunnable);
    }
    
    private void readInputFromFirebase() {
        // Don't read input if we don't have the other player's ID yet
        if (otherPlayerId == null) {
            return;
        }
        
        // Read input from other player (only for shooting state, position comes from gameState)
        // Note: Position updates are handled by updateGameState() which reads from gameState
        // This method is kept for potential future use or can be removed if not needed
        DatabaseReference otherPlayerInputRef = inputRef.child(otherPlayerId);
        otherPlayerInputRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();
                // Only read shooting state here if needed, position comes from gameState
                Boolean shooting = snapshot.child("shooting").getValue(Boolean.class);
                if (shooting != null) {
                    otherPlayerShooting = shooting;
                }
                // Position and angle are updated from gameState in updateGameState()
            }
        });
    }
    
    private void sendInputToFirebase() {
        // Always send our current position and aim so host can mirror quickly
        Map<String, Object> input = new HashMap<>();
        input.put("angle", rightAngle);
        input.put("shooting", isShooting);
        input.put("x", playerX);
        input.put("y", playerY);
        input.put("timestamp", System.currentTimeMillis());
        inputRef.child(playerId).setValue(input);
    }
    
    private void updateGameStateInFirebase() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("level", currentLevel);
        updates.put("exp", currentExp);
        updates.put("gameOver", gameOver);
        
        if (isHost) {
            // Only send host position if it's valid (not 0,0) and within bounds
            // This prevents guest from seeing ghost position
            if (isValidPosition(playerX, playerY) && playerX != 0f && playerY != 0f) {
                updates.put("hostPlayerX", playerX);
                updates.put("hostPlayerY", playerY);
            }
            updates.put("hostPlayerAngle", rightAngle);
            updates.put("hostPlayerShooting", isShooting);
        } else {
            // Only send guest position if it's valid (not 0,0) and within bounds
            // This prevents host from seeing ghost position
            if (isValidPosition(playerX, playerY) && playerX != 0f && playerY != 0f) {
                updates.put("guestPlayerX", playerX);
                updates.put("guestPlayerY", playerY);
            }
            updates.put("guestPlayerAngle", rightAngle);
            updates.put("guestPlayerShooting", isShooting);
        }
        
        gameStateRef.updateChildren(updates);
    }
    
    private void updateEnemiesAndProjectilesInFirebase() {
        if (!isHost) return; // Only host updates enemies and projectiles
        
        Map<String, Object> enemies = new HashMap<>();
        for (ImageView enemy : activeEnemies) {
            String enemyId = enemyIdMap.get(enemy);
            if (enemyId == null) continue; // Skip if no ID assigned
            
            Map<String, Object> enemyData = new HashMap<>();
            enemyData.put("x", enemy.getX());
            enemyData.put("y", enemy.getY());
            enemyData.put("size", enemy.getWidth());
            enemies.put(enemyId, enemyData);
        }
        
        Map<String, Object> projectiles = new HashMap<>();
        for (ImageView projectile : activeProjectiles) {
            String projectileId = projectileIdMap.get(projectile);
            if (projectileId == null) continue; // Skip if no ID assigned (guest projectiles)
            
            Map<String, Object> projectileData = new HashMap<>();
            projectileData.put("x", projectile.getX());
            projectileData.put("y", projectile.getY());
            projectileData.put("size", projectile.getWidth());
            projectiles.put(projectileId, projectileData);
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("enemies", enemies);
        updates.put("projectiles", projectiles);
        
        gameStateRef.updateChildren(updates);
    }
    
    private void gainExperience() {
        currentExp++;
        
        if (currentExp >= expToNextLevel) {
            currentExp = 0;
            currentLevel++;
            expToNextLevel += Math.max(1, (int) (expToNextLevel * 0.2f));
        }
        
        // Update UI
        runOnUiThread(() -> {
            levelText.setText("Level " + currentLevel);
            updateExpBar();
        });
    }
    
    private void updateExpBar() {
        if (expBarProgress != null) {
            float progress = (float) currentExp / expToNextLevel;
            int containerWidth = expBarProgress.getParent() instanceof View ? 
                ((View) expBarProgress.getParent()).getWidth() : 400;
            if (containerWidth > 0) {
                int progressWidth = (int) (containerWidth * progress);
                expBarProgress.getLayoutParams().width = progressWidth;
                expBarProgress.requestLayout();
            }
        }
    }
    
    private void endGame() {
        gameOver = true;
        
        // Update Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameOver", true);
        gameStateRef.updateChildren(updates);
        
        // Show game over UI
        runOnUiThread(() -> {
            TextView gameOverText = new TextView(this);
            gameOverText.setText("GAME OVER\nLevel: " + currentLevel);
            gameOverText.setTextSize(24);
            gameOverText.setTextColor(0xFFFFFFFF);
            gameOverText.setBackgroundColor(0x80000000);
            gameOverText.setPadding(50, 50, 50, 50);
            gameOverText.setGravity(android.view.Gravity.CENTER);
            
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
            params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            
            gameOverText.setLayoutParams(params);
            mainLayout.addView(gameOverText);
            
            // Return to menu after 3 seconds
            gameHandler.postDelayed(() -> {
                Intent intent = new Intent(MultiplayerGameActivity.this, MainMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }, 3000);
        });
    }
    
    private boolean handleTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        // Determine which control pair is being touched
        if (isTouchInLeftControl(x, y)) {
            // Left control - movement direction
            float centerX = leftBigCircle.getX() + leftBigCircle.getWidth() / 2f;
            float centerY = leftBigCircle.getY() + leftBigCircle.getHeight() / 2f;
            
            float dx = x - centerX;
            float dy = y - centerY;
            
            leftAngle = Math.atan2(dy, dx);
            
            // Update left small circle position
            updateLeftSmallCircle();
            
        } else if (isTouchInRightControl(x, y)) {
            // Right control - shooting direction
            float centerX = rightBigCircle.getX() + rightBigCircle.getWidth() / 2f;
            float centerY = rightBigCircle.getY() + rightBigCircle.getHeight() / 2f;
            
            float dx = x - centerX;
            float dy = y - centerY;
            
            rightAngle = Math.atan2(dy, dx);
            
            // Update right small circle position
            updateRightSmallCircle();
            
            // Set shooting state
            isShooting = event.getAction() == MotionEvent.ACTION_DOWN || 
                         event.getAction() == MotionEvent.ACTION_MOVE;
        }
        
        return true;
    }
    
    private boolean isTouchInLeftControl(float x, float y) {
        float centerX = leftBigCircle.getX() + leftBigCircle.getWidth() / 2f;
        float centerY = leftBigCircle.getY() + leftBigCircle.getHeight() / 2f;
        float radius = 100f; // Touch radius
        
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance <= radius;
    }
    
    private boolean isTouchInRightControl(float x, float y) {
        float centerX = rightBigCircle.getX() + rightBigCircle.getWidth() / 2f;
        float centerY = rightBigCircle.getY() + rightBigCircle.getHeight() / 2f;
        float radius = 100f; // Touch radius
        
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance <= radius;
    }
    
    private void updateLeftSmallCircle() {
        float centerX = leftBigCircle.getX() + leftBigCircle.getWidth() / 2f;
        float centerY = leftBigCircle.getY() + leftBigCircle.getHeight() / 2f;
        
        float radius = 100f;
        float smallX = centerX + (float) (Math.cos(leftAngle) * radius) - leftSmallCircle.getWidth() / 2f;
        float smallY = centerY + (float) (Math.sin(leftAngle) * radius) - leftSmallCircle.getHeight() / 2f;
        
        leftSmallCircle.setX(smallX);
        leftSmallCircle.setY(smallY);
    }
    
    private void updateRightSmallCircle() {
        float centerX = rightBigCircle.getX() + rightBigCircle.getWidth() / 2f;
        float centerY = rightBigCircle.getY() + rightBigCircle.getHeight() / 2f;
        
        float radius = 100f;
        float smallX = centerX + (float) (Math.cos(rightAngle) * radius) - rightSmallCircle.getWidth() / 2f;
        float smallY = centerY + (float) (Math.sin(rightAngle) * radius) - rightSmallCircle.getHeight() / 2f;
        
        rightSmallCircle.setX(smallX);
        rightSmallCircle.setY(smallY);
    }
    
    private boolean viewsIntersect(View a, View b) {
        float ax = a.getX();
        float ay = a.getY();
        float aw = a.getWidth();
        float ah = a.getHeight();
        float bx = b.getX();
        float by = b.getY();
        float bw = b.getWidth();
        float bh = b.getHeight();
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
    
    private String getOtherPlayerId() {
        return otherPlayerId;
    }
    
    private void updateEnemiesFromSnapshot(DataSnapshot snapshot) {
        // Sync enemy positions from host
        Log.d(TAG, "Guest: updateEnemiesFromSnapshot called");
        DataSnapshot enemiesSnapshot = snapshot.child("enemies");
        Log.d(TAG, "Guest: enemiesSnapshot.exists() = " + enemiesSnapshot.exists());
        
        if (enemiesSnapshot.exists()) {
            int enemyCount = 0;
            // Track which enemies exist in the snapshot
            HashSet<String> existingEnemyIds = new HashSet<>();
            
            // Update or create enemies from snapshot
            for (DataSnapshot enemySnapshot : enemiesSnapshot.getChildren()) {
                enemyCount++;
                String enemyId = enemySnapshot.getKey();
                if (enemyId == null) continue;
                
                existingEnemyIds.add(enemyId);
                
                Float x = enemySnapshot.child("x").getValue(Float.class);
                Float y = enemySnapshot.child("y").getValue(Float.class);
                Integer size = enemySnapshot.child("size").getValue(Integer.class);
                
                if (x != null && y != null && size != null) {
                    ImageView enemy = syncedEnemies.get(enemyId);
                    
                    if (enemy == null) {
                        // Create new enemy only if position is valid
                        if (isValidPosition(x, y)) {
                            Log.d(TAG, "Guest: Creating enemy " + enemyId + " at (" + x + ", " + y + ")");
                            enemy = new ImageView(this);
                            enemy.setLayoutParams(new ConstraintLayout.LayoutParams(size, size));
                            enemy.setImageResource(R.drawable.enemy_square);
                            enemy.setX(x);
                            enemy.setY(y);
                            mainLayout.addView(enemy);
                            syncedEnemies.put(enemyId, enemy);
                            activeEnemies.add(enemy);
                            // Track the position we set
                            lastEnemyPositions.put(enemyId, new float[]{x, y});
                        } else {
                            Log.d(TAG, "Guest: Rejected creating enemy " + enemyId + " - invalid position (" + x + ", " + y + ")");
                        }
                    } else {
                        // Get last known position (not from view, which might be stale)
                        float[] lastPos = lastEnemyPositions.get(enemyId);
                        float lastX = lastPos != null ? lastPos[0] : enemy.getX();
                        float lastY = lastPos != null ? lastPos[1] : enemy.getY();
                        
                        // Use tolerance for position comparison (avoid floating point precision issues)
                        float positionTolerance = 0.5f;
                        float dx = Math.abs(lastX - x);
                        float dy = Math.abs(lastY - y);
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);
                        boolean positionChanged = (dx > positionTolerance || dy > positionTolerance);
                        
                        // Check if position jump is too large (indicates invalid/corrupted data from Firebase)
                        // Reject jumps > 50 pixels (enemies normally move 1-5 pixels per frame)
                        // 50 pixels allows for some network delay/batching but rejects obvious invalid data
                        boolean reasonableJump = distance <= 50f;
                        
                        Log.d(TAG, "Guest: Enemy " + enemyId + " - Firebase: (" + x + ", " + y + "), Last: (" + 
                                  lastX + ", " + lastY + "), Changed: " + positionChanged + ", Distance: " + distance +
                                  ", Reasonable: " + reasonableJump);
                        
                        // Only update size if it changed
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) enemy.getLayoutParams();
                        if (params.width != size || params.height != size) {
                            params.width = size;
                            params.height = size;
                            enemy.setLayoutParams(params);
                        }
                        
                        // Only update position if it changed significantly AND is valid AND jump is reasonable
                        // If position is invalid or jump is too large, keep current position (don't update)
                        if (isValidPosition(x, y) && reasonableJump) {
                            if (positionChanged) {
                                Log.d(TAG, "Guest: UPDATING enemy " + enemyId + " position from (" + lastX + ", " + lastY + ") to (" + x + ", " + y + ")");
                                enemy.setX(x);
                                enemy.setY(y);
                                // Update tracked position
                                lastEnemyPositions.put(enemyId, new float[]{x, y});
                            } else {
                                Log.d(TAG, "Guest: Enemy " + enemyId + " position unchanged, skipping update");
                            }
                        } else {
                            if (!isValidPosition(x, y)) {
                                Log.d(TAG, "Guest: Enemy " + enemyId + " - invalid position (" + x + ", " + y + "), keeping current");
                            } else if (!reasonableJump) {
                                Log.d(TAG, "Guest: Enemy " + enemyId + " - jump too large (" + distance + " pixels), keeping current position");
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Guest: Processed " + enemyCount + " enemies from snapshot, existingEnemyIds size: " + existingEnemyIds.size());
            
            // Reset missing frames counter for enemies that appear in snapshot
            for (String enemyId : existingEnemyIds) {
                enemyMissingFrames.remove(enemyId); // Reset counter if enemy appears
            }
            
            // Track missing enemies - only remove after missing for multiple consecutive frames
            ArrayList<String> enemiesToRemove = new ArrayList<>();
            for (String enemyId : syncedEnemies.keySet()) {
                if (!existingEnemyIds.contains(enemyId)) {
                    // Enemy is missing - increment missing frames counter
                    int missingFrames = enemyMissingFrames.getOrDefault(enemyId, 0) + 1;
                    enemyMissingFrames.put(enemyId, missingFrames);
                    
                    Log.d(TAG, "Guest: Enemy " + enemyId + " missing from snapshot (frame " + missingFrames + "/" + MISSING_FRAMES_THRESHOLD + ")");
                    
                    // Only remove if missing for threshold number of frames
                    if (missingFrames >= MISSING_FRAMES_THRESHOLD) {
                        ImageView enemy = syncedEnemies.get(enemyId);
                        if (enemy != null && enemy.getParent() != null) {
                            mainLayout.removeView(enemy);
                            activeEnemies.remove(enemy);
                        }
                        enemiesToRemove.add(enemyId);
                        enemyMissingFrames.remove(enemyId); // Clean up counter
                        Log.d(TAG, "Guest: REMOVING enemy " + enemyId + " (missing for " + missingFrames + " frames)");
                    }
                }
            }
            for (String enemyId : enemiesToRemove) {
                syncedEnemies.remove(enemyId);
                lastEnemyPositions.remove(enemyId); // Clean up tracked position
            }
        } else {
            // No enemies in snapshot - increment missing frames for all enemies
            // Only remove if missing for threshold frames (prevents flickering when snapshot is temporarily empty)
            ArrayList<String> enemiesToRemove = new ArrayList<>();
            for (String enemyId : syncedEnemies.keySet()) {
                int missingFrames = enemyMissingFrames.getOrDefault(enemyId, 0) + 1;
                enemyMissingFrames.put(enemyId, missingFrames);
                
                if (missingFrames >= MISSING_FRAMES_THRESHOLD) {
                    ImageView enemy = syncedEnemies.get(enemyId);
                    if (enemy != null && enemy.getParent() != null) {
                        mainLayout.removeView(enemy);
                        activeEnemies.remove(enemy);
                    }
                    enemiesToRemove.add(enemyId);
                    enemyMissingFrames.remove(enemyId);
                }
            }
            for (String enemyId : enemiesToRemove) {
                syncedEnemies.remove(enemyId);
                lastEnemyPositions.remove(enemyId);
            }
            
            // Only clear everything if all enemies have been removed
            if (syncedEnemies.isEmpty()) {
                enemyMissingFrames.clear();
                Log.d(TAG, "Guest: enemiesSnapshot does not exist in Firebase - all enemies removed");
            } else {
                Log.d(TAG, "Guest: enemiesSnapshot does not exist - " + syncedEnemies.size() + " enemies waiting for removal threshold");
            }
        }
    }
    
    private void updateProjectilesFromSnapshot(DataSnapshot snapshot) {
        // Sync projectile positions from host
        DataSnapshot projectilesSnapshot = snapshot.child("projectiles");
        if (projectilesSnapshot.exists()) {
            // Track which projectiles exist in the snapshot
            HashSet<String> existingProjectileIds = new HashSet<>();
            
            // Update or create projectiles from snapshot
            for (DataSnapshot projectileSnapshot : projectilesSnapshot.getChildren()) {
                String projectileId = projectileSnapshot.getKey();
                if (projectileId == null) continue;
                
                existingProjectileIds.add(projectileId);
                
                Float x = projectileSnapshot.child("x").getValue(Float.class);
                Float y = projectileSnapshot.child("y").getValue(Float.class);
                Integer size = projectileSnapshot.child("size").getValue(Integer.class);
                
                if (x != null && y != null && size != null) {
                    ImageView projectile = syncedProjectiles.get(projectileId);
                    
                    if (projectile == null) {
                        // Create new projectile only if position is valid
                        if (isValidPosition(x, y)) {
                            projectile = new ImageView(this);
                            projectile.setLayoutParams(new ConstraintLayout.LayoutParams(size, size));
                            projectile.setImageResource(R.drawable.circle);
                            projectile.setX(x);
                            projectile.setY(y);
                            mainLayout.addView(projectile);
                            syncedProjectiles.put(projectileId, projectile);
                            activeProjectiles.add(projectile);
                            // Track the position we set
                            lastProjectilePositions.put(projectileId, new float[]{x, y});
                        }
                    } else {
                        // Get last known position (not from view, which might be stale)
                        float[] lastPos = lastProjectilePositions.get(projectileId);
                        float lastX = lastPos != null ? lastPos[0] : projectile.getX();
                        float lastY = lastPos != null ? lastPos[1] : projectile.getY();
                        
                        // Use tolerance for position comparison (avoid floating point precision issues)
                        float positionTolerance = 0.5f;
                        float dx = Math.abs(lastX - x);
                        float dy = Math.abs(lastY - y);
                        boolean positionChanged = (dx > positionTolerance || dy > positionTolerance);
                        
                        // Only update size if it changed
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) projectile.getLayoutParams();
                        if (params.width != size || params.height != size) {
                            params.width = size;
                            params.height = size;
                            projectile.setLayoutParams(params);
                        }
                        
                        // Only update position if it changed significantly AND is valid
                        // If position is invalid, keep current position (don't update)
                        if (isValidPosition(x, y)) {
                            if (positionChanged) {
                                projectile.setX(x);
                                projectile.setY(y);
                                // Update tracked position
                                lastProjectilePositions.put(projectileId, new float[]{x, y});
                            }
                            // If position hasn't changed, don't update (prevents flickering)
                        }
                        // If position is invalid, do nothing - keep current position
                    }
                }
            }
            
            // Remove projectiles that no longer exist in the snapshot
            ArrayList<String> projectilesToRemove = new ArrayList<>();
            for (String projectileId : syncedProjectiles.keySet()) {
                if (!existingProjectileIds.contains(projectileId)) {
                    ImageView projectile = syncedProjectiles.get(projectileId);
                    if (projectile != null && projectile.getParent() != null) {
                        mainLayout.removeView(projectile);
                        activeProjectiles.remove(projectile);
                    }
                    projectilesToRemove.add(projectileId);
                }
            }
            for (String projectileId : projectilesToRemove) {
                syncedProjectiles.remove(projectileId);
                lastProjectilePositions.remove(projectileId); // Clean up tracked position
            }
        } else {
            // No projectiles in snapshot - remove all synced projectiles
            for (ImageView projectile : syncedProjectiles.values()) {
                if (projectile != null && projectile.getParent() != null) {
                    mainLayout.removeView(projectile);
                    activeProjectiles.remove(projectile);
                }
            }
            syncedProjectiles.clear();
            lastProjectilePositions.clear(); // Clean up all tracked positions
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (gameStateListener != null && gameStateRef != null) {
            gameStateRef.removeEventListener(gameStateListener);
        }
        
        if (gameHandler != null) {
            gameHandler.removeCallbacksAndMessages(null);
        }
        
        // Clean up Firebase references
        if (isHost) {
            gameStateRef.removeValue();
        }
        
        inputRef.child(playerId).removeValue();
    }
}
