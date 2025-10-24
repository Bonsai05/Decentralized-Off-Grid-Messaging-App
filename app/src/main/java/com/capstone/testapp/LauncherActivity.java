package com.capstone.testapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- NEW: Check and generate crypto keys on first launch ---
        CryptoManager cryptoManager = new CryptoManager(this);
        if (!cryptoManager.areKeysGenerated()) {
            cryptoManager.generateAndSaveKeys();
        }
        // --- END OF NEW BLOCK ---

        // This part remains the same
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("UserName", null);

        Intent intent;
        if (savedName == null) {
            // No name saved, go to SetupActivity
            intent = new Intent(this, SetupActivity.class);
        } else {
            // Name exists, go to MainActivity
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish(); // Close this activity immediately
    }
}