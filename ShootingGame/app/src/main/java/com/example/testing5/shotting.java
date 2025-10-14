package com.example.testing5;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
		final int[] contentWidth = {0};
		final int[] contentHeight = {0};
		final boolean[] shootingStarted = {false};
        Handler handler = new Handler();
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
                //Log.d("tag",mirrorPair.small.getY()+" "+projectile.getY());
				final float speedPxPerSecond = 600f;
				final long frameMs = 16;
				final float dx = (float) (Math.cos(angle) * speedPxPerSecond * (frameMs / 1000f));
				final float dy = (float) (Math.sin(angle) * speedPxPerSecond * (frameMs / 1000f));

				Handler projectileHandler = new Handler();
				Runnable moveProjectile = new Runnable() {
					@Override
					public void run() {
						projectile.setX(projectile.getX() + dx);
						projectile.setY(projectile.getY() + dy);
						float x = projectile.getX();
						float y = projectile.getY();
						if (x < -projectileSize || x > contentWidth[0] + projectileSize || y < -projectileSize || y > contentHeight[0] + projectileSize) {
							mainLayout.removeView(projectile);
							return;
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
		mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				contentWidth[0] = mainLayout.getWidth();
				contentHeight[0] = mainLayout.getHeight();
				if (!shootingStarted[0] && contentWidth[0] > 0 && contentHeight[0] > 0) {
					shootingStarted[0] = true;
					handler.post(shootRunnable);
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