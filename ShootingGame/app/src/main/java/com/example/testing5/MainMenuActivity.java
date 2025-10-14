package com.example.testing5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_layout);

        // Get button references
        Button startButton = findViewById(R.id.startButton);
        Button quitButton = findViewById(R.id.quitButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);

        // Start button - navigate to game
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, shotting.class);
                startActivity(intent);
            }
        });

        // Quit button - exit app
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Settings button - show settings (placeholder)
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainMenuActivity.this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
