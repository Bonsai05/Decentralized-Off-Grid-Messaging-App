package com.capstone.testapp;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// The @Database annotation marks this as a Room database class.
// 'entities' lists all the table classes.
// 'version' is for database migrations; start with 1.
// FIX 1: The version number is increased from 1 to 2
@Database(entities = {Message.class, Contact.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // An abstract method for Room to provide an instance of our DAO.
    public abstract MessageDao messageDao();
    public abstract ContactDao contactDao();

    // This is the Singleton pattern. It prevents multiple instances of the
    // database opening at the same time.
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "chat_database")
                            // FIX 2: Add this line to handle the version change
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}