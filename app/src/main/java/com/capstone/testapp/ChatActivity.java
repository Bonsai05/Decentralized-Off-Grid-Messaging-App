package com.capstone.testapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    // --- UI, Database, and Crypto variables ---
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private AppDatabase db;
    private MessageDao messageDao;
    private ExecutorService executorService;
    private CryptoManager cryptoManager;
    private String contactPublicKey;

    // --- BLE variables ---
    private static final String TAG = "ChatActivity_BLE";
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID RX_CHARACTERISTIC_UUID = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private boolean isScanning = false;
    private Handler scanHandler;

    // --- Fully implemented Activity Result Launchers ---
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startScanning();
                } else {
                    Toast.makeText(this, "Bluetooth is required to chat.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> blePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (boolean isGranted : result.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    checkBluetoothState();
                } else {
                    Toast.makeText(this, "All permissions are required to chat.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scanHandler = new Handler(Looper.getMainLooper());

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        cryptoManager = new CryptoManager(this);
        Intent intent = getIntent();
        String contactName = intent.getStringExtra("CONTACT_NAME");
        contactPublicKey = intent.getStringExtra("CONTACT_PUBLIC_KEY");
        if (contactPublicKey == null) {
            Toast.makeText(this, "Error: Contact's public key not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(contactName);
        }

        db = AppDatabase.getDatabase(this);
        messageDao = db.messageDao();
        executorService = Executors.newSingleThreadExecutor();
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        loadMessages();

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        checkPermissions();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void sendMessage(String messageText) {
        Message newMessage = new Message(messageText, System.currentTimeMillis(), true);
        executorService.execute(() -> {
            messageDao.insert(newMessage);
            runOnUiThread(() -> {
                messageList.add(newMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            });
        });

        String encryptedMessage = cryptoManager.encrypt(messageText, contactPublicKey);
        if (encryptedMessage != null) {
            sendBleMessage(encryptedMessage);
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Encryption Failed!", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadMessages() {
        executorService.execute(() -> {
            List<Message> loadedMessages = messageDao.getAllMessages();
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(loadedMessages);
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    private void checkPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest = new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT };
        } else {
            permissionsToRequest = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
        }
        blePermissionsLauncher.launch(permissionsToRequest);
    }

    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return;
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;

        if (isScanning) return;

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanHandler.postDelayed(() -> {
            if (isScanning) {
                Log.d(TAG, "Scan timed out. Stopping scan.");
                stopScan();
                runOnUiThread(() -> Toast.makeText(this, "Node not found.", Toast.LENGTH_SHORT).show());
            }
        }, 50000); // 10 second scan timeout

        isScanning = true;
        // Scan WITHOUT filters - we will filter manually in the callback
        scanner.startScan(null, scanSettings, scanCallback);
        Log.d(TAG, "Scan started (no filter)...");
        Toast.makeText(this, "Searching for your LoRa Node...", Toast.LENGTH_SHORT).show();
    }

    private void stopScan() {
        if (isScanning && scanner != null) {
            isScanning = false;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;
            scanner.stopScan(scanCallback);
            scanHandler.removeCallbacksAndMessages(null);
        }
    }

    private void sendBleMessage(String encryptedText) {
        if (gatt == null) {
            Toast.makeText(this, "Not connected to LoRa Node", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) { Log.e(TAG, "Service not found!"); return; }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID);
        if (characteristic == null) { Log.e(TAG, "TX Characteristic not found!"); return; }

        byte[] messageBytes = encryptedText.getBytes(StandardCharsets.UTF_8);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, messageBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            characteristic.setValue(messageBytes);
            gatt.writeCharacteristic(characteristic);
        }
        runOnUiThread(() -> messageEditText.setText(""));
    }

    private void processScanResult(ScanResult result) {
        if (!isScanning) return;

        BluetoothDevice device = result.getDevice();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        String deviceName = device.getName();
        Log.d(TAG, "Found BLE device: " + (deviceName != null ? deviceName : "Unnamed") + " [" + device.getAddress() + "]");

        // Check if the scan result contains our Service UUID
        List<ParcelUuid> serviceUuids = result.getScanRecord() != null ? result.getScanRecord().getServiceUuids() : null;
        boolean foundOurService = false;
        if (serviceUuids != null) {
            for (ParcelUuid uuid : serviceUuids) {
                if (uuid.getUuid().equals(SERVICE_UUID)) {
                    foundOurService = true;
                    break;
                }
            }
        }

        if (foundOurService) {
            // We found a device advertising our service!
            stopScan();
            Log.d(TAG, "TARGET SERVICE FOUND! Connecting to " + (deviceName != null ? deviceName : device.getAddress()));

            // Connect to the device
            gatt = device.connectGatt(ChatActivity.this, false, gattCallback);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!isScanning) return;
            Log.d(TAG, "Received a batch of " + results.size() + " scan results.");
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with error code: " + errorCode);
            isScanning = false;
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to LoRa Node.");
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connected to Node!", Toast.LENGTH_SHORT).show());
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.discoverServices();
            }
            // Add handling for disconnection if needed
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from LoRa Node.");
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Disconnected from Node.", Toast.LENGTH_SHORT).show());
                ChatActivity.this.gatt = null; // Clear the gatt object
                // Optionally, restart scanning if desired:
                // checkPermissions();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered. Enabling notifications...");
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service == null) { Log.e(TAG,"Service UUID not found!"); return; }
                BluetoothGattCharacteristic rxChar = service.getCharacteristic(RX_CHARACTERISTIC_UUID);
                if (rxChar == null) { Log.e(TAG,"RX Characteristic UUID not found!"); return; }

                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.setCharacteristicNotification(rxChar, true);

                BluetoothGattDescriptor descriptor = rxChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (descriptor == null) { Log.e(TAG,"CCC Descriptor not found!"); return; }

                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                Log.d(TAG, "Enabled notifications for RX characteristic.");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RX_CHARACTERISTIC_UUID)) {
                String encryptedMessage = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                Log.d(TAG, "Received encrypted message from node.");

                String decryptedMessage = cryptoManager.decrypt(encryptedMessage);
                if (decryptedMessage != null) {
                    Log.d(TAG, "Decryption successful.");
                    Message receivedMessage = new Message(decryptedMessage, System.currentTimeMillis(), false);
                    executorService.execute(() -> {
                        messageDao.insert(receivedMessage);
                        runOnUiThread(() -> {
                            messageList.add(receivedMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        });
                    });
                } else {
                    Log.e(TAG, "Decryption failed!");
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
            gatt.close();
            gatt = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

