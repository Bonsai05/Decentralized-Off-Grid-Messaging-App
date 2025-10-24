package com.capstone.testapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {
    @Insert
    void insert(Contact contact);

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    List<Contact> getAllContacts();
}