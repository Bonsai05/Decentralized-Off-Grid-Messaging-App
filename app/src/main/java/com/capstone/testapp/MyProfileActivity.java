package com.capstone.testapp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject; // Import the JSONObject class

public class MyProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);

        try {
            // Get user's name from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String myName = prefs.getString("UserName", "DefaultName");

            // Get user's public key from CryptoManager
            CryptoManager cryptoManager = new CryptoManager(this);
            String myPublicKey = cryptoManager.getPublicKeyString();

            if (myPublicKey == null) {
                Toast.makeText(this, "Error: Could not find public key.", Toast.LENGTH_LONG).show();
                return;
            }

            // Create a JSON object to hold both pieces of data
            JSONObject qrCodeData = new JSONObject();
            qrCodeData.put("name", myName);
            qrCodeData.put("publicKey", myPublicKey);

            // Convert the JSON object to a string to be encoded
            String qrCodeString = qrCodeData.toString();

            // Generate the QR code from the JSON string
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrCodeString, com.google.zxing.BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not generate QR Code", Toast.LENGTH_SHORT).show();
        }
    }
}