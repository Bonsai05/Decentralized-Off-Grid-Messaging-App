package com.capstone.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject; // Add this import for JSON

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private RecyclerView contactsRecyclerView;
    private FloatingActionButton addContactFab;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;

    // Database
    private AppDatabase db;
    private ContactDao contactDao;
    private ExecutorService executorService;

    // The launcher now expects to receive JSON data from the QR code
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String qrCodeData = result.getContents(); // This is a JSON string
                    saveNewContact(qrCodeData);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addContactFab = findViewById(R.id.addContactFab);
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        db = AppDatabase.getDatabase(this);
        contactDao = db.contactDao();
        executorService = Executors.newSingleThreadExecutor();

        contactList = new ArrayList<>();

        // --- UPDATED: The click listener now also sends the public key ---
        contactAdapter = new ContactAdapter(contactList, contact -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("CONTACT_NAME", contact.name);
            intent.putExtra("CONTACT_PUBLIC_KEY", contact.publicKey); // Add this line
            startActivity(intent);
        });

        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(contactAdapter);

        loadContacts();

        addContactFab.setOnClickListener(v -> {
            launchScanner();
        });
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a friend's QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private void loadContacts() {
        executorService.execute(() -> {
            List<Contact> loadedContacts = contactDao.getAllContacts();
            runOnUiThread(() -> {
                contactList.clear();
                contactList.addAll(loadedContacts);
                contactAdapter.notifyDataSetChanged();
            });
        });
    }

    // --- UPDATED: This method now parses JSON data ---
    private void saveNewContact(String qrCodeData) {
        try {
            // Parse the JSON string from the QR code
            JSONObject json = new JSONObject(qrCodeData);
            String name = json.getString("name");
            String publicKey = json.getString("publicKey");

            executorService.execute(() -> {
                // Save both name and public key using the updated constructor
                Contact newContact = new Contact(name, publicKey);
                contactDao.insert(newContact);

                runOnUiThread(this::loadContacts);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Contact '" + name + "' saved!", Toast.LENGTH_SHORT).show();
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Menu Methods ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_my_profile) {
            Intent intent = new Intent(this, MyProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}