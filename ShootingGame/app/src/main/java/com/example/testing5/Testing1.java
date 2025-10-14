package com.example.testing5;

import android.content.Intent;
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
import androidx.constraintlayout.widget.ConstraintSet;

public class Testing1 extends AppCompatActivity {


    private View circleView;
    private EditText numberEditText;
    private Button moveButton;
    private ConstraintLayout mainLayout;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Handler delayHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable, delayRunnable;
    private boolean isPaused = false;


    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0; // nếu không phải số, trả về 0
        }
    }

    private void moveCircle(int hight) {
        // 3. Get the text from the EditText
        if (hight==0) {
            return; // Do nothing if the input is empty
        }

        try {

            // 5. Update the circle's position
            // We use a ConstraintSet to change the layout properties programmatically
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mainLayout); // Copy existing constraints

            // Change the vertical bias of the circle view
            constraintSet.setVerticalBias(R.id.circle_view,hight/100f);

            // Apply the new constraints to the layout
            constraintSet.applyTo(mainLayout);

        } catch (NumberFormatException e) {
            // Handle cases where the input is not a valid number, e.g., show a toast
            //numberEditText.setError("Invalid number");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start the timer when the activity becomes visible
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the timer when the activity is no longer visible to save resources
        timerHandler.removeCallbacks(timerRunnable);
    }




    int count = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_testing1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainLayout = findViewById(R.id.main);
        circleView = findViewById(R.id.circle_view);
        numberEditText = findViewById(R.id.editTextNumberDecimal);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            if (!isPaused) {
                isPaused = true;
                // Resume automatically after 1 second
                timerHandler.postDelayed(() -> isPaused = false, 1000);
            }
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Increment the count
                if (!isPaused) {
                    count++;

                    // Keep the count within a reasonable range for the circle animation (e.g., 0-100)
                    if (count > 100) {
                        count = 0; // Reset to 0 after it reaches 100
                    }

                    // Update the EditText with the new count value
                    numberEditText.setText(String.valueOf(count));


                }
                // Schedule this same runnable to be executed again after 1 second
                timerHandler.postDelayed(this, 100);
            }
        };

        numberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
//                int number = parseIntSafe(s.toString());
//                if(number <100 ) {
//                    numberEditText.setText(String.valueOf(number + 1));
//                }else{
//                    numberEditText.setText(String.valueOf(number - 1));
//                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int number = parseIntSafe(s.toString());
                moveCircle(number);
            }
        });



    }



}