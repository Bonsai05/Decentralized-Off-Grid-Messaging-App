package com.capstone.testapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

// The @Dao annotation identifies this as a Data Access Object for Room.
@Dao
public interface MessageDao {

    // The @Insert annotation creates a command to insert a new message.
    @Insert
    void insert(Message message);

    // The @Query annotation lets you write custom SQL to read from the database.
    // This query selects all messages and orders them by their timestamp.
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessages();
}