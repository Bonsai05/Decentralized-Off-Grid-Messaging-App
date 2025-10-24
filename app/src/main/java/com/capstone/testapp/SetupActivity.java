package com.capstone.testapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        EditText nameEditText = findViewById(R.id.nameEditText);
        Button saveNameButton = findViewById(R.id.saveNameButton);

        saveNameButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            } else {
                // Save the name to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("UserName", name);
                editor.apply();

                // Go to MainActivity
                Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close this activity so the user can't come back to it
            }
        });
    }
}