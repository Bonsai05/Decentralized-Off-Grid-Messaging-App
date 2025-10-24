package com.capstone.testapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String textContent;
    public long timestamp;

    // --- THIS FIELD WAS ADDED ---
    public boolean isSentByMe;

    // --- THIS CONSTRUCTOR WAS UPDATED ---
    public Message(String textContent, long timestamp, boolean isSentByMe) {
        this.textContent = textContent;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }
}

