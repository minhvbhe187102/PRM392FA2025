package com.example.testing5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class Slot2Activity extends AppCompatActivity {

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0; // nếu không phải số, trả về 0
        }
    }

    private EditText editTextNumber;
    private EditText editTextNumber3;
    private EditText editTextNumber4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = new Intent(Slot2Activity.this, Testing1.class);
        startActivity(intent);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_slot2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //

        editTextNumber = findViewById(R.id.editTextNumber);
        editTextNumber3 = findViewById(R.id.editTextNumber3);
        editTextNumber4 = findViewById(R.id.editTextNumber4);




        Button button = findViewById(R.id.button);

        //
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text1 = editTextNumber.getText().toString();
                String text2 = editTextNumber3.getText().toString();



                int num1 = parseIntSafe(text1);
                int num2 = parseIntSafe(text2);
                int sum = num1 + num2;
                editTextNumber4.setText(String.valueOf(sum)); // ghi
                if (sum == 10) {
                    Intent intent = new Intent(Slot2Activity.this, Testing1.class);
                    startActivity(intent);
                }
            }
        });
    }
}
