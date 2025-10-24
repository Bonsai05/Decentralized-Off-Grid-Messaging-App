package com.capstone.testapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

public class BleActivity extends AppCompatActivity {

    private static final String TAG = "BleActivity";
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer gattServer;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;

    // --- NEW: Launcher to request turning on Bluetooth ---
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth Enabled!", Toast.LENGTH_SHORT).show();
                    checkPermissions(); // Now that BT is on, check for other permissions
                } else {
                    Toast.makeText(this, "Bluetooth is required for this feature.", Toast.LENGTH_LONG).show();
                }
            });

    // --- UPDATED: Launcher to request BLE permissions with better feedback ---
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
                    Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Some permissions were denied. The app may not function correctly.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        findViewById(R.id.advertiseButton).setOnClickListener(v -> startAdvertising());
        findViewById(R.id.scanButton).setOnClickListener(v -> {
            if (scanner != null) {
                stopScanning();
            } else {
                startScanning();
            }
        });
        
        // Add a test button for unfiltered scanning
        findViewById(R.id.scanButton).setOnLongClickListener(v -> {
            startUnfilteredScanning();
            return true;
        });
        
        // Add a comprehensive device discovery test
        findViewById(R.id.advertiseButton).setOnLongClickListener(v -> {
            startComprehensiveDeviceDiscovery();
            return true;
        });

        // --- UPDATED: Start the check process in onCreate ---
        checkBluetoothState();
        
        // Test BLE functionality
        testBleCapabilities();
        
        // Add comprehensive system debugging
        performSystemBleDiagnostics();
    }

    // --- NEW: Step 1 - Check if Bluetooth is on ---
    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkPermissions(); // If BT is already on, proceed to check permissions
        }
    }

    // --- UPDATED: Step 2 - Check for necessary permissions ---
    private void checkPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest = new String[]{ 
                Manifest.permission.BLUETOOTH_SCAN, 
                Manifest.permission.BLUETOOTH_CONNECT, 
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION // Still needed for older devices
            };
        } else {
            permissionsToRequest = new String[]{ 
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            };
        }
        
        // Check which permissions are already granted
        boolean allGranted = true;
        for (String permission : permissionsToRequest) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            Log.d(TAG, "All permissions already granted");
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Requesting permissions: " + java.util.Arrays.toString(permissionsToRequest));
            blePermissionsLauncher.launch(permissionsToRequest);
        }
    }

    private void testBleCapabilities() {
        Log.d(TAG, "Testing BLE capabilities...");
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null - BLE not supported");
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled");
            return;
        }
        
        // Test if BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BLE feature not supported");
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Test if advertising is supported
        boolean advertisingSupported = bluetoothAdapter.isMultipleAdvertisementSupported();
        Log.d(TAG, "BLE Advertising supported: " + advertisingSupported);
        
        // Test if we can get a scanner
        BluetoothLeScanner testScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (testScanner == null) {
            Log.e(TAG, "Cannot create BLE scanner");
            Toast.makeText(this, "Cannot create BLE scanner", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "BLE scanner creation successful");
        }
        
        Log.d(TAG, "BLE capability test completed");
        
        // Check Android version specific issues
        checkAndroidVersionIssues();
    }
    
    private void checkAndroidVersionIssues() {
        Log.d(TAG, "Checking Android version specific issues...");
        Log.d(TAG, "Android SDK Version: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Android Version: " + Build.VERSION.RELEASE);
        
        // Check location services for Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean locationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || 
                                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
            Log.d(TAG, "Location services enabled: " + locationEnabled);
            
            if (!locationEnabled) {
                Log.w(TAG, "Location services are disabled - this may prevent BLE scanning on Android 6.0+");
                Toast.makeText(this, "Location services disabled - may affect BLE scanning", Toast.LENGTH_LONG).show();
            }
        }
        
        // Check for Android 12+ specific issues
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Android 12+ detected - using new BLE permissions");
            
            // Check if we have the new BLE permissions
            boolean hasBluetoothScan = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasBluetoothConnect = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean hasBluetoothAdvertise = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
            
            Log.d(TAG, "BLUETOOTH_SCAN permission: " + hasBluetoothScan);
            Log.d(TAG, "BLUETOOTH_CONNECT permission: " + hasBluetoothConnect);
            Log.d(TAG, "BLUETOOTH_ADVERTISE permission: " + hasBluetoothAdvertise);
        }
        
        // Check for Android 10+ background location restrictions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "Android 10+ detected - background location restrictions may apply");
        }
    }
    
    private void performSystemBleDiagnostics() {
        Log.d(TAG, "=== COMPREHENSIVE BLE SYSTEM DIAGNOSTICS ===");
        
        // 1. Check Bluetooth adapter state
        Log.d(TAG, "Bluetooth Adapter State:");
        Log.d(TAG, "  - Adapter exists: " + (bluetoothAdapter != null));
        if (bluetoothAdapter != null) {
            Log.d(TAG, "  - Enabled: " + bluetoothAdapter.isEnabled());
            Log.d(TAG, "  - State: " + bluetoothAdapter.getState());
            Log.d(TAG, "  - Address: " + bluetoothAdapter.getAddress());
            Log.d(TAG, "  - Name: " + bluetoothAdapter.getName());
            Log.d(TAG, "  - Multiple advertisement supported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            Log.d(TAG, "  - Le 2M PHY supported: " + bluetoothAdapter.isLe2MPhySupported());
            Log.d(TAG, "  - Le Coded PHY supported: " + bluetoothAdapter.isLeCodedPhySupported());
            Log.d(TAG, "  - Le Extended advertising supported: " + bluetoothAdapter.isLeExtendedAdvertisingSupported());
        }
        
        // 2. Check BLE scanner creation
        Log.d(TAG, "BLE Scanner Test:");
        BluetoothLeScanner testScanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;
        Log.d(TAG, "  - Scanner created: " + (testScanner != null));
        
        // 3. Check system permissions in detail
        Log.d(TAG, "Permission Status:");
        String[] allPermissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        for (String permission : allPermissions) {
            int status = ActivityCompat.checkSelfPermission(this, permission);
            Log.d(TAG, "  - " + permission + ": " + 
                (status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }
        
        // 4. Check location services
        Log.d(TAG, "Location Services:");
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            Log.d(TAG, "  - GPS Provider: " + locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
            Log.d(TAG, "  - Network Provider: " + locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER));
            Log.d(TAG, "  - Passive Provider: " + locationManager.isProviderEnabled(android.location.LocationManager.PASSIVE_PROVIDER));
        }
        
        // 5. Check device capabilities
        Log.d(TAG, "Device Capabilities:");
        PackageManager pm = getPackageManager();
        Log.d(TAG, "  - BLE Feature: " + pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        Log.d(TAG, "  - Bluetooth Feature: " + pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
        Log.d(TAG, "  - WiFi Feature: " + pm.hasSystemFeature(PackageManager.FEATURE_WIFI));
        
        // 6. Check Android version and API level
        Log.d(TAG, "Android Version Info:");
        Log.d(TAG, "  - SDK Version: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "  - Release: " + Build.VERSION.RELEASE);
        Log.d(TAG, "  - Codename: " + Build.VERSION.CODENAME);
        Log.d(TAG, "  - Brand: " + Build.BRAND);
        Log.d(TAG, "  - Model: " + Build.MODEL);
        Log.d(TAG, "  - Manufacturer: " + Build.MANUFACTURER);
        
        // 7. Test a simple scan to see if it starts
        Log.d(TAG, "Testing Simple Scan Start:");
        if (testScanner != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                ScanSettings testSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
                
                // Create a test callback that just logs
                ScanCallback testCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.d(TAG, "TEST SCAN: Found device " + result.getDevice().getAddress());
                    }
                    
                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e(TAG, "TEST SCAN FAILED: " + errorCode);
                    }
                };
                
                testScanner.startScan(null, testSettings, testCallback);
                Log.d(TAG, "  - Test scan started successfully");
                
                // Stop the test scan after 2 seconds
                new android.os.Handler().postDelayed(() -> {
                    try {
                        testScanner.stopScan(testCallback);
                        Log.d(TAG, "  - Test scan stopped");
                    } catch (Exception e) {
                        Log.e(TAG, "  - Error stopping test scan: " + e.getMessage());
                    }
                }, 2000);
                
            } catch (Exception e) {
                Log.e(TAG, "  - Test scan failed to start: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "  - Cannot start test scan - scanner or permission issue");
        }
        
        Log.d(TAG, "=== END BLE SYSTEM DIAGNOSTICS ===");
    }
    
    private void startComprehensiveDeviceDiscovery() {
        Log.d(TAG, "=== STARTING COMPREHENSIVE DEVICE DISCOVERY ===");
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted");
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "Failed to create scanner");
            return;
        }

        // Test different scan modes
        ScanSettings[] testSettings = {
            new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .build(),
            new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(0)
                .build(),
            new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
        };

        String[] modeNames = {"LOW_POWER", "BALANCED", "LOW_LATENCY"};
        
        for (int i = 0; i < testSettings.length; i++) {
            final int modeIndex = i;
            Log.d(TAG, "Testing scan mode: " + modeNames[i]);
            
            ScanCallback discoveryCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    int rssi = result.getRssi();
                    
                    Log.d(TAG, "DISCOVERY SCAN [" + modeNames[modeIndex] + "]:");
                    Log.d(TAG, "  Device: " + (deviceName != null ? deviceName : "Unknown"));
                    Log.d(TAG, "  Address: " + deviceAddress);
                    Log.d(TAG, "  RSSI: " + rssi);
                    
                    if (result.getScanRecord() != null) {
                        Log.d(TAG, "  Service UUIDs: " + result.getScanRecord().getServiceUuids());
                        Log.d(TAG, "  Device Name: " + result.getScanRecord().getDeviceName());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "Discovery scan [" + modeNames[modeIndex] + "] failed: " + errorCode);
                }
            };
            
            try {
                scanner.startScan(null, testSettings[i], discoveryCallback);
                Log.d(TAG, "Started discovery scan with " + modeNames[i] + " mode");
                
                // Stop after 3 seconds
                new android.os.Handler().postDelayed(() -> {
                    try {
                        scanner.stopScan(discoveryCallback);
                        Log.d(TAG, "Stopped discovery scan with " + modeNames[modeIndex] + " mode");
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping discovery scan: " + e.getMessage());
                    }
                }, 3000);
                
                // Wait between scans
                Thread.sleep(3500);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start discovery scan with " + modeNames[i] + ": " + e.getMessage());
            }
        }
        
        Log.d(TAG, "=== COMPREHENSIVE DEVICE DISCOVERY COMPLETED ===");
    }

    private void startAdvertising() {
        // PERMISSION CHECK
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Advertise permission needed.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- NEW: Check if advertising is supported by the hardware ---
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "BLE Advertising not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            Toast.makeText(this, "Failed to create advertiser", Toast.LENGTH_SHORT).show();
            return;
        }

        gattServer = bluetoothManager.openGattServer(this, gattServerCallback);
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                MESSAGE_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(characteristic);
        gattServer.addService(service);

        AdvertiseSettings settings = new AdvertiseSettings.Builder().build();
        AdvertiseData data = new AdvertiseData.Builder().addServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
        advertiser.startAdvertising(settings, data, advertiseCallback);
        Toast.makeText(this, "Advertising Started", Toast.LENGTH_SHORT).show();
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() { /* ... no changes here ... */ };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        // ... no changes needed in onConnectionStateChange ...

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (characteristic.getUuid().equals(MESSAGE_CHARACTERISTIC_UUID)) {
                String message = new String(value, StandardCharsets.UTF_8);
                runOnUiThread(() -> Toast.makeText(BleActivity.this, "Received: " + message, Toast.LENGTH_SHORT).show());

                if (responseNeeded) {
                    if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            }
        }
    };

    private void startScanning() {
        Log.d(TAG, "Starting BLE scan...");
        
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted");
            Toast.makeText(this, "Scan permission needed.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        // Check if scanner was created successfully
        if (scanner == null) {
            Log.e(TAG, "Failed to create scanner - BLE not supported or Bluetooth off");
            Toast.makeText(this, "Failed to create scanner", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Scanner created successfully, starting scan...");
        
        // Create scan filter for our service UUID
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        
        // Create scan settings with better configuration
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0) // Report results immediately
                .build();
        
        try {
            scanner.startScan(Collections.singletonList(scanFilter), scanSettings, scanCallback);
            Log.d(TAG, "Scan started successfully");
            Toast.makeText(this, "Scanning Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception starting scan: " + e.getMessage());
            Toast.makeText(this, "Failed to start scan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startUnfilteredScanning() {
        Log.d(TAG, "Starting UNFILTERED BLE scan...");
        
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted");
            Toast.makeText(this, "Scan permission needed.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "Failed to create scanner - BLE not supported or Bluetooth off");
            Toast.makeText(this, "Failed to create scanner", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Scanner created successfully, starting UNFILTERED scan...");
        
        // Create scan settings with different modes
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // Try low power mode
                .setReportDelay(0) // Report results immediately
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        
        try {
            // Scan WITHOUT any filters - this should find ALL BLE devices
            scanner.startScan(null, scanSettings, unfilteredScanCallback);
            Log.d(TAG, "Unfiltered scan started successfully");
            Toast.makeText(this, "Unfiltered Scanning Started - Check Logs", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception starting unfiltered scan: " + e.getMessage());
            Toast.makeText(this, "Failed to start unfiltered scan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopScanning() {
        Log.d(TAG, "Stopping BLE scan...");
        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback);
                Log.d(TAG, "Scan stopped successfully");
                Toast.makeText(this, "Scanning Stopped", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Exception stopping scan: " + e.getMessage());
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            int rssi = result.getRssi();
            
            Log.d(TAG, "FILTERED SCAN RESULT:");
            Log.d(TAG, "  Device: " + (deviceName != null ? deviceName : "Unknown"));
            Log.d(TAG, "  Address: " + deviceAddress);
            Log.d(TAG, "  RSSI: " + rssi);
            Log.d(TAG, "  Callback Type: " + callbackType);
            
            // Log scan record details
            if (result.getScanRecord() != null) {
                Log.d(TAG, "  Scan Record:");
                Log.d(TAG, "    Device Name: " + result.getScanRecord().getDeviceName());
                Log.d(TAG, "    Service UUIDs: " + result.getScanRecord().getServiceUuids());
                Log.d(TAG, "    Service Data: " + result.getScanRecord().getServiceData());
                Log.d(TAG, "    Manufacturer Data: " + result.getScanRecord().getManufacturerSpecificData());
            }
            
            if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_SCAN permission not granted");
                return;
            }
            scanner.stopScan(this);
            
            Log.d(TAG, "Connecting to device: " + device.getAddress());
            if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                return;
            }
            gatt = device.connectGatt(BleActivity.this, false, gattCallback);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error code: " + errorCode);
            runOnUiThread(() -> {
                String errorMessage = "Scan failed";
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        errorMessage = "Scan already started";
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        errorMessage = "Application registration failed";
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage = "BLE scanning not supported";
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        errorMessage = "Internal error";
                        break;
                }
                Toast.makeText(BleActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            });
        }
    };

    private final ScanCallback unfilteredScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            int rssi = result.getRssi();
            
            Log.d(TAG, "UNFILTERED SCAN RESULT:");
            Log.d(TAG, "  Device: " + (deviceName != null ? deviceName : "Unknown"));
            Log.d(TAG, "  Address: " + deviceAddress);
            Log.d(TAG, "  RSSI: " + rssi);
            Log.d(TAG, "  Callback Type: " + callbackType);
            
            // Log scan record details
            if (result.getScanRecord() != null) {
                Log.d(TAG, "  Scan Record:");
                Log.d(TAG, "    Device Name: " + result.getScanRecord().getDeviceName());
                Log.d(TAG, "    Service UUIDs: " + result.getScanRecord().getServiceUuids());
                Log.d(TAG, "    Service Data: " + result.getScanRecord().getServiceData());
                Log.d(TAG, "    Manufacturer Data: " + result.getScanRecord().getManufacturerSpecificData());
            }
            
            // Check if this is our target service
            if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                    if (uuid.getUuid().equals(SERVICE_UUID)) {
                        Log.d(TAG, "*** FOUND TARGET SERVICE! ***");
                        runOnUiThread(() -> Toast.makeText(BleActivity.this, 
                            "Found target service on " + (deviceName != null ? deviceName : deviceAddress), 
                            Toast.LENGTH_LONG).show());
                        
                        // Stop scanning and connect
                        if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;
                        scanner.stopScan(this);
                        
                        if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                        gatt = device.connectGatt(BleActivity.this, false, gattCallback);
                        return;
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Unfiltered scan failed with error code: " + errorCode);
            runOnUiThread(() -> {
                String errorMessage = "Unfiltered scan failed: ";
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        errorMessage += "Scan already started";
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        errorMessage += "Application registration failed";
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += "BLE scanning not supported";
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        errorMessage += "Internal error";
                        break;
                }
                Toast.makeText(BleActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            });
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        sendMessage(gatt, characteristic);
                    }
                }
            }
        }
    };

    private void sendMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        byte[] messageBytes = "Hello BLE".getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, messageBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            characteristic.setValue(messageBytes);
            gatt.writeCharacteristic(characteristic);
        }
        Log.d(TAG, "Sent message: Hello BLE");
    }
}