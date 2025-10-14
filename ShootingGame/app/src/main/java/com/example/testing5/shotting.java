package com.example.testing5;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class shotting extends AppCompatActivity {

    class CirclePair {
        View big;
        View small;
        double currentAngle;
        boolean isMoving;
        int pointerId = -1; // -1 means not finger controlled
        boolean fingerin;

        CirclePair(View big, View small) {
            this.big = big;
            this.small = small;
            this.currentAngle = 0;
            this.isMoving = false;
        }
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

    private final Handler moveHandler = new Handler();
    private Runnable moveRunnable,moveRunnable2;

    private ImageView bigCircle; // your large circle reference



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shotting);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			// Do not add padding to the game root; keep a stable (0,0) in parent coords
			return insets;
		});



        ImageView bigCircle = findViewById(R.id.imageView);
        ImageView smallCircle = findViewById(R.id.imageView2);
        ImageView bigCircle2 = findViewById(R.id.imageView3);
        ImageView smallCircle2 = findViewById(R.id.imageView4);
        ImageView bigCircle3 = findViewById(R.id.imageView5);
        ImageView smallCircle3 = findViewById(R.id.imageView6);
		ConstraintLayout mainLayout = findViewById(R.id.main);
		
		// Experience bar UI
		View expBarContainer = findViewById(R.id.expBarContainer);
		View expBarProgress = findViewById(R.id.expBarProgress);
		TextView levelText = findViewById(R.id.levelText);
		
		final int[] contentWidth = {0};
		final int[] contentHeight = {0};
		final boolean[] shootingStarted = {false};
        final boolean[] enemyStarted = {false};
        final List<ImageView> activeEnemies = new ArrayList<>();
        final List<ImageView> activeProjectiles = new ArrayList<>();
        final Map<ImageView, Integer> enemyHp = new HashMap<>();
        final Map<ImageView, Float> enemySpeedMap = new HashMap<>();
        
        // Experience system
        final int[] currentExp = {0};
        final int[] currentLevel = {1};
        final int[] expToNextLevel = {3}; // Start with 3 exp needed
        
        // Game state
        final boolean[] gameOver = {false};
        
        // Update experience bar and level
        final Runnable updateExpBar = new Runnable() {
            @Override
            public void run() {
                if (expBarContainer != null && expBarProgress != null && levelText != null) {
                    // Calculate progress percentage
                    float progress = Math.min(1.0f, (float) currentExp[0] / expToNextLevel[0]);
                    
                    // Update progress bar width
                    int containerWidth = expBarContainer.getWidth();
                    if (containerWidth > 0) {
                        int progressWidth = (int) (containerWidth * progress);
                        expBarProgress.getLayoutParams().width = progressWidth;
                        expBarProgress.requestLayout();
                    }
                    
                    // Update level text
                    levelText.setText("Level " + currentLevel[0]);
                }
            }
        };
        Handler handler = new Handler();
        // Gain experience when enemy dies
        final Runnable gainExp = new Runnable() {
            @Override
            public void run() {
                currentExp[0]++;
                
                // Check if we can level up
                if (currentExp[0] >= expToNextLevel[0]) {
                    currentExp[0] = 0; // Reset exp
                    currentLevel[0]++;
                    // Increase exp requirement by 1.2x, minimum 1
                    expToNextLevel[0] += Math.max(1, (int) (expToNextLevel[0] * 0.2f));
                }
                
                // Update UI
                handler.post(updateExpBar);
            }
        };
        
        // Forward declarations to avoid circular dependency
        final int[] enemySpawnIntervalMs = {5000};
        Runnable enemySpawnRunnable;
        Runnable showGameOver;
        
        // Initialize showGameOver first since it's used in enemySpawnRunnable
        showGameOver = new Runnable() {
            @Override
            public void run() {
                if (gameOver[0]) return; // Prevent multiple calls
                gameOver[0] = true;

                // Stop all game loops
                handler.removeCallbacksAndMessages(null);

                // Show game over layout
                View gameOverView = getLayoutInflater().inflate(R.layout.game_over_layout, mainLayout, false);
                gameOverView.setId(R.id.gameOverLayout);
                TextView finalLevelText = gameOverView.findViewById(R.id.finalLevelText);
                finalLevelText.setText("Final Level: " + currentLevel[0]);

                Button restartButton = gameOverView.findViewById(R.id.restartButton);
                restartButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Restart activity using Intent
                        Intent intent = new Intent(shotting.this, shotting.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
                
                Button returnToMenuButton = gameOverView.findViewById(R.id.returnToMenuButton);
                returnToMenuButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Return to main menu
                        Intent intent = new Intent(shotting.this, MainMenuActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });

                mainLayout.addView(gameOverView);
            }
        };
        
        // Spawns enemies at a decreasing interval (start 5s, minus 0.5s each spawn, min 1s)
        enemySpawnRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameOver[0]) return; // Stop spawning if game over
                if (contentWidth[0] <= 0 || contentHeight[0] <= 0) {
                    // try again next tick if layout not ready
                    handler.postDelayed(this, 1000);
                    return;
                }

                final ImageView enemy = new ImageView(shotting.this);
                enemy e = com.example.testing5.enemy.randomExample();
                if (e == null) {
                    e = new enemy(0, "fallback", 1, 10, 10);
                }
                float density = getResources().getDisplayMetrics().density;
                int enemySizePx = (int) (e.getSize() * 2.4f * density); // 10 units => 24dp
                enemy.setLayoutParams(new ConstraintLayout.LayoutParams(enemySizePx, enemySizePx));
                enemy.setImageResource(R.drawable.enemy_square);
                // Spawn randomly just outside one screen edge
                int edge = (int) (Math.random() * 4); // 0=left,1=top,2=right,3=bottom
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
                mainLayout.addView(enemy);
                activeEnemies.add(enemy);
                enemyHp.put(enemy, e.getHp());
                float speedPxPerSecond = e.getSpd() * 25f; // 10 spd => 250 px/s
                enemySpeedMap.put(enemy, speedPxPerSecond);

                final long enemyFrameMs = 16;
                Runnable enemyFollow = new Runnable() {
                    @Override
                    public void run() {
                        if (gameOver[0] || enemy.getParent() == null) {
                            return;
                        }
                        float enemySpeed = enemySpeedMap.containsKey(enemy) ? enemySpeedMap.get(enemy) : 250f;
                        float targetX = bigCircle3.getX() + bigCircle3.getWidth() / 2f;
                        float targetY = bigCircle3.getY() + bigCircle3.getHeight() / 2f;
                        float ex = enemy.getX() + enemy.getWidth() / 2f;
                        float ey = enemy.getY() + enemy.getHeight() / 2f;
                        float dx = targetX - ex;
                        float dy = targetY - ey;
                        float dist = (float) Math.hypot(dx, dy);
                        if (dist > 1f) {
                            float step = enemySpeed * (enemyFrameMs / 1000f);
                            float nx = dx / dist;
                            float ny = dy / dist;
                            enemy.setX(enemy.getX() + nx * step);
                            enemy.setY(enemy.getY() + ny * step);
                        }
                        
                        // Check collision with player
                        if (viewsIntersect(enemy, bigCircle3)) {
                            // Game over!
                            handler.post(showGameOver);
                            return;
                        }
                        enemy.postDelayed(this, enemyFrameMs);
                    }
                };
                enemy.post(enemyFollow);

                // schedule next spawn with decreasing interval, min 1s
                enemySpawnIntervalMs[0] = Math.max(3000, enemySpawnIntervalMs[0] - 500);
                handler.postDelayed(this, enemySpawnIntervalMs[0]);
            }
        };
        //Handler handler = new Handler();
        final int[] spawnCount = {0}; // track number of circles spawned
        final float startX = 100;
        final float startY = 100;



// Start spawning after 5 seconds
        //handler.postDelayed(spawnRunnable, 1000);

// Use wrappers for mutable values
        CirclePair leftPair = new CirclePair(bigCircle, smallCircle);      // left
        CirclePair rightPair = new CirclePair(bigCircle2, smallCircle2);   // right
        CirclePair mirrorPair  = new CirclePair(bigCircle3, smallCircle3); // 3rd pair, follows left

        List<CirclePair> circlePairs = new ArrayList<>();
        circlePairs.add(leftPair);
        circlePairs.add(rightPair);

		// Shooter: fire a small circle from the middle (mirrorPair) every second
		// Content bounds in parent's coordinate space (excludes status bar/action bar)
		// Will be filled after layout pass
        //Log.d("TAG", getResources().getDisplayMetrics().widthPixels+" "+getResources().getDisplayMetrics().heightPixels);
		Runnable shootRunnable = new Runnable() {
			@Override
			public void run() {
				if (gameOver[0]) return; // Stop shooting if game over
				final int projectileSize = 32;
				ImageView projectile = new ImageView(shotting.this);
				projectile.setLayoutParams(new ConstraintLayout.LayoutParams(projectileSize, projectileSize));
				projectile.setImageResource(R.drawable.circle);

				float angle = (float) mirrorPair.currentAngle;
				// Start from the small circle's center so it's always tangent, no hardcoded offsets
				float startX = mirrorPair.small.getX() + mirrorPair.small.getWidth() / 2f - projectileSize / 2f;
				float startY = mirrorPair.small.getY() + mirrorPair.small.getHeight() / 2f - projectileSize / 2f;


				projectile.setX(startX);
				projectile.setY(startY);
				mainLayout.addView(projectile);
                activeProjectiles.add(projectile);
                //Log.d("tag",mirrorPair.small.getY()+" "+projectile.getY());
				final float speedPxPerSecond = 600f;
				final long frameMs = 16;
				final float dx = (float) (Math.cos(angle) * speedPxPerSecond * (frameMs / 1000f));
				final float dy = (float) (Math.sin(angle) * speedPxPerSecond * (frameMs / 1000f));

                Handler projectileHandler = new Handler();
				Runnable moveProjectile = new Runnable() {
					@Override
					public void run() {
                        if (gameOver[0] || projectile.getParent() == null) {
                            return;
                        }
						projectile.setX(projectile.getX() + dx);
						projectile.setY(projectile.getY() + dy);
						float x = projectile.getX();
						float y = projectile.getY();
                        // Bounds check
                        if (x < -projectileSize || x > contentWidth[0] + projectileSize || y < -projectileSize || y > contentHeight[0] + projectileSize) {
                            mainLayout.removeView(projectile);
                            activeProjectiles.remove(projectile);
                            return;
                        }
                        // Collision check with enemies
                        for (int i = activeEnemies.size() - 1; i >= 0; i--) {
                            ImageView enemyView = activeEnemies.get(i);
                            if (enemyView.getParent() == null) {
                                activeEnemies.remove(i);
                                enemyHp.remove(enemyView);
                                enemySpeedMap.remove(enemyView);
                                continue;
                            }
                            if (viewsIntersect(projectile, enemyView)) {
                                mainLayout.removeView(projectile);
                                activeProjectiles.remove(projectile);
                                Integer hp = enemyHp.get(enemyView);
                                if (hp == null) hp = 1;
                                hp -= 1;
                                if (hp <= 0) {
                                    mainLayout.removeView(enemyView);
                                    activeEnemies.remove(i);
                                    enemyHp.remove(enemyView);
                                    enemySpeedMap.remove(enemyView);
                                    // Gain experience when enemy dies
                                    handler.post(gainExp);
                                } else {
                                    enemyHp.put(enemyView, hp);
                                }
                                return;
                            }
                        }
						projectileHandler.postDelayed(this, frameMs);
					}
				};
				projectileHandler.post(moveProjectile);
//
				// Schedule next shot
				handler.postDelayed(this, 1000);
			}
		};

		// Wait until layout is done to get accurate content size and start shooter only once
		mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				contentWidth[0] = mainLayout.getWidth();
				contentHeight[0] = mainLayout.getHeight();
				if (!shootingStarted[0] && contentWidth[0] > 0 && contentHeight[0] > 0) {
					shootingStarted[0] = true;
					handler.post(shootRunnable);
				}

                // Start periodic enemy spawns when layout is ready
                if (!enemyStarted[0] && contentWidth[0] > 0 && contentHeight[0] > 0) {
                    enemyStarted[0] = true;
                    handler.post(enemySpawnRunnable); // first spawn immediately, then every 5s
                }
                
                // Initialize exp bar
                handler.post(updateExpBar);

				// Remove listener after first pass when both have started
				if (shootingStarted[0] && enemyStarted[0]) {
					mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			}
		});


        Runnable moveRunnable = new Runnable() {
            @Override
            public void run() {
                if (leftPair.isMoving) {
                    // Calculate offset of left small circle from its big circle
                    float leftCx = leftPair.big.getX() + leftPair.big.getWidth() / 2f;
                    float leftCy = leftPair.big.getY() + leftPair.big.getHeight() / 2f;

                    float smallCx = leftCx + (float)Math.cos(leftPair.currentAngle) * leftPair.big.getWidth() / 2f;
                    float smallCy = leftCy + (float)Math.sin(leftPair.currentAngle) * leftPair.big.getWidth() / 2f;

                    float dx = smallCx - leftCx;
                    float dy = smallCy - leftCy;

                    // Move 3rd big circle
                    mirrorPair.big.setX(mirrorPair.big.getX() + dx/10);
                    mirrorPair.big.setY(mirrorPair.big.getY() + dy/10);

                    // Update 3rd small circle rotation
                    float mirrorCx = mirrorPair.big.getX() + mirrorPair.big.getWidth() / 2f;
                    float mirrorCy = mirrorPair.big.getY() + mirrorPair.big.getHeight() / 2f;
                    float radius = mirrorPair.big.getWidth() / 2f;
                    float newSmallX = (float)(mirrorCx + (radius * Math.cos(mirrorPair.currentAngle) - mirrorPair.small.getWidth()/2f)/1);
                    float newSmallY = (float)(mirrorCy + (radius * Math.sin(mirrorPair.currentAngle) - mirrorPair.small.getHeight()/2f)/1);
                    mirrorPair.small.setX(newSmallX);
                    mirrorPair.small.setY(newSmallY);

                    // Repeat
                    handler.postDelayed(this, 30);
                }
            }
        };



// Note: mirrorPair is not assigned to fingers, so we don't add it her
        mainLayout.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            int pointerIndex = event.getActionIndex();

            switch (action) {

                case MotionEvent.ACTION_DOWN:

                case MotionEvent.ACTION_POINTER_DOWN: {

                    //Log.d("TAG", " ok ");
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);

                    // Assign finger to closest pair (left or right)
                    CirclePair closestPair = null;
                    float minDistance = Float.MAX_VALUE;

                    for (CirclePair pair : Arrays.asList(leftPair, rightPair)) {
                        if (!pair.isMoving) {
                            float cx = pair.big.getX() + pair.big.getWidth() / 2f;
                            float cy = pair.big.getY() + pair.big.getHeight() / 2f;
                            float dx = x - cx;
                            float dy = y - cy;
                            float distance = dx * dx + dy * dy;

                            if (distance < minDistance) {
                                minDistance = distance;
                                closestPair = pair;
                            }
                        }
                    }

                    if (closestPair != null) {
                        closestPair.isMoving = true;
                        closestPair.fingerin = true;
                        closestPair.pointerId = event.getPointerId(pointerIndex);

                        float cx = closestPair.big.getX() + closestPair.big.getWidth() / 2f;
                        float cy = closestPair.big.getY() + closestPair.big.getHeight() / 2f;
                        closestPair.currentAngle = Math.atan2(y - cy, x - cx);
                        if (closestPair == leftPair) {
                            handler.post(moveRunnable);
                        }
                    }

                    break;
                }

                case MotionEvent.ACTION_MOVE:

                    //Log.d("TAG", " moving ");
                    // Move left and right pairs by finger
                    for (CirclePair pair : Arrays.asList(leftPair, rightPair)) {
                        if (pair.isMoving) {
                            int idx = event.findPointerIndex(pair.pointerId);
                            if (idx != -1) {
                                float x = event.getX(idx);
                                float y = event.getY(idx);

                                float cx = pair.big.getX() + pair.big.getWidth() / 2f;
                                float cy = pair.big.getY() + pair.big.getHeight() / 2f;

                                pair.currentAngle = Math.atan2(y - cy, x - cx);

                                float radius = pair.big.getWidth() / 2f;
                                float newX = (float) (cx + radius * Math.cos(pair.currentAngle) - pair.small.getWidth() / 2f);
                                float newY = (float) (cy + radius * Math.sin(pair.currentAngle) - pair.small.getHeight() / 2f);

                                pair.small.setX(newX);
                                pair.small.setY(newY);
                            }
                        }
                    }

                    // NEW: Left pair also moves the big circle of the 3rd pair


                    // Mirror the angle of right pair to the 3rd pair's small circle if needed
                {
                    mirrorPair.currentAngle = rightPair.currentAngle; // optional, depends on desired behavior
                    float cx = mirrorPair.big.getX() + mirrorPair.big.getWidth() / 2f;
                    float cy = mirrorPair.big.getY() + mirrorPair.big.getHeight() / 2f;
                    float radius = mirrorPair.big.getWidth() / 2f;
                    float newSmallX = (float) (cx + radius * Math.cos(mirrorPair.currentAngle) - mirrorPair.small.getWidth() / 2f);
                    float newSmallY = (float) (cy + radius * Math.sin(mirrorPair.currentAngle) - mirrorPair.small.getHeight() / 2f);
                    mirrorPair.small.setX(newSmallX);
                    mirrorPair.small.setY(newSmallY);
                }

                break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    int releasedPointerId = event.getPointerId(pointerIndex);
                    for (CirclePair pair : Arrays.asList(leftPair, rightPair)) {
                        if (pair.pointerId == releasedPointerId) {
                            pair.isMoving = false;
                            pair.fingerin = false;
                            pair.pointerId = -1;
                            //Log.d("TAG", "NOT ok ");
                        }
                    }
                    break;
                }
            }

            return true;
        });





    }
}