package com.capstone.testapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class Contact {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String publicKey; // This field is now added

    // The constructor is updated to accept the publicKey
    public Contact(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }
}