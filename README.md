# Decentralized Off-Grid Messenger (D.O.M.)

> **A Capstone Android + IoT Project** — Secure, offline communication using LoRa and Bluetooth Low Energy (BLE), powered by end-to-end encryption.

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Android App — “LoRa BLE Encrypted Messenger”](#android-app--lora-ble-encrypted-messenger)

   * [Built With](#built-with)
   * [Core Features](#-core-features)
4. [App Structure](#app-structure)
5. [Key Components](#key-components)
6. [Hardware Integration (Node Prototype)](#-hardware-integration-node-prototype)
7. [Project Phases](#project-phases)
8. [Future Enhancements](#future-enhancements)
9. [Authors](#authors)
10. [License](#license)

---

## Overview

**Decentralized Off-Grid Messenger (D.O.M.)** is a hybrid communication system combining **LoRa hardware nodes** and an **Android companion app**.
It enables secure, long-range, and offline messaging even without internet access — ideal for disaster zones, remote regions, or privacy-focused users.

The project integrates:

* **LoRa** for long-range, low-power communication
* **Bluetooth LE** for device pairing and relay
* **RSA encryption** for end-to-end data privacy
* **Local storage** using Room Database
* **QR-based public key sharing**

---

## System Architecture

```
+------------------+     Bluetooth LE     +-----------------+     LoRa     +-----------------+     Bluetooth LE     +------------------+
|   User A Phone   | <------------------> |   User A Node   | <---------> |   User B Node   | <------------------> |   User B Phone   |
| (Companion App)  |                      |    (Hardware)   |              |    (Hardware)   |                      | (Companion App)  |
+------------------+                      +-----------------+              +-----------------+                      +------------------+
```

Messages hop across **phones**, **nodes**, and **mules** to form a **resilient mesh network**.

---

## Android App — “LoRa BLE Encrypted Messenger”

### Built With

* **Language:** Java
* **IDE:** Android Studio
* **Database:** Room (SQLite ORM)
* **QR Scanning:** ZXing Embedded Library
* **Encryption:** RSA (2048-bit)
* **Minimum SDK:** 23
* **Target SDK:** 36

---

## Core Features

| Feature                   | Description                                                            |
| ------------------------- | ---------------------------------------------------------------------- |
| **End-to-End Encryption** | RSA-based keypair generation and secure message encryption/decryption. |
| **Offline Messaging**     | Send and receive text messages without internet, via BLE and LoRa.     |
| **QR Code Contacts**      | Exchange public keys securely using QR codes.                          |
| **Mule Mode**             | Relay messages across nearby users to extend network range.            |
| **Local Storage**         | Store messages and contacts using Room Database.                       |
| **Auto Key Management**   | Keys generated automatically on first app launch.                      |

---

## App Structure

```
app/
├── java/com/capstone/testapp/
│   ├── activities/
│   │   ├── LauncherActivity.java
│   │   ├── SetupActivity.java
│   │   ├── MainActivity.java
│   │   ├── ChatActivity.java
│   │   └── MyProfileActivity.java
│   ├── adapters/
│   │   ├── MessageAdapter.java
│   │   └── ContactAdapter.java
│   ├── database/
│   │   ├── AppDatabase.java
│   │   ├── Message.java
│   │   ├── Contact.java
│   │   ├── MessageDao.java
│   │   └── ContactDao.java
│   ├── helpers/
│   │   └── CryptoManager.java
│   └── utils/
│       └── Constants.java
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── activity_chat.xml
│   │   ├── activity_my_profile.xml
│   │   ├── activity_setup.xml
│   │   ├── message_item.xml
│   │   └── contact_item.xml
│   ├── drawable/
│   │   ├── bg_bubble_sent.xml
│   │   └── bg_bubble_received.xml
│   ├── values/
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── menu/
│       └── main_menu.xml
│
└── AndroidManifest.xml
```

---

## Key Components

| Component                  | Description                                                 |
| -------------------------- | ----------------------------------------------------------- |
| **CryptoManager.java**     | Handles RSA keypair generation, encryption, and decryption. |
| **ChatActivity.java**      | Core chat logic, BLE scanning, and message exchange.        |
| **MainActivity.java**      | Displays contacts and manages QR-based contact addition.    |
| **MyProfileActivity.java** | Generates user QR code containing name and public key.      |
| **Room Database**          | Stores messages and contacts locally.                       |

---

## Hardware Integration (Node Prototype)

| Component                   | Purpose                                |
| --------------------------- | -------------------------------------- |
| **ESP32**                   | Core microcontroller with BLE and WiFi |
| **LoRa SX1276 Module**      | Long-range data transmission           |
| **TP4056 + 18650 Cell**     | Power and charging circuit             |
| **LCD Display**             | Display device status                  |
| **Antenna**                 | Frequency-tuned LoRa antenna           |

---

## Project Phases

1. **Hardware Prototyping** — Build and validate ESP32 + LoRa node.
2. **Firmware Development** — Implement LoRa/BLE message handling.
3. **Companion App Development** — Android-side encrypted chat and QR system.
4. **Mesh Integration** — Store-and-forward message routing between nodes.
5. **Testing & Deployment** — Real-world mesh testing and optimization.

---

## Future Enhancements

* Hybrid mesh using WiFi Direct + BLE
* Gateway mode for Internet fallback
* Voice or file message support
* Web dashboard for monitoring LoRa nodes
* Integration with Firebase or MQTT bridge

---

## Authors

**Team D.O.M.**
 Suraj Layak
 Siddhesh Durgude
 Atharva Raut
 Yash Rathod

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---
