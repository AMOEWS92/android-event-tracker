package com.example.cs360_eventtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "event_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Table for user accounts
        String createUsersTable =
                "CREATE TABLE users(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT NOT NULL UNIQUE, " +
                        "password_hash TEXT NOT NULL, " +
                        "password_salt TEXT NOT NULL)";

        db.execSQL(createUsersTable);

        // Tables for saved events
        String createEventsTable =
                "CREATE TABLE events(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "name TEXT NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "location TEXT, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id) " +
                        "ON DELETE CASCADE)";
        db.execSQL(createEventsTable);
    }

    public long addEvent(Long userId, String name,
                         String date,String location) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        if (userId != null) {
            values.put("user_id", userId);
        }

        values.put("name", name);
        values.put("date", date);
        values.put("location", location);

        return db.insert("events", null, values);
    }


    public Cursor getEvents(Long userId) {

        SQLiteDatabase db = getReadableDatabase();

        String selection;
        String[] selectionArgs;

        if (userId == null) {
            selection = "user_id IS NULL";
            selectionArgs = null;
        } else {
            selection = "user_id = ?";
            selectionArgs = new String[]{String.valueOf(userId)};
        }

        return db.query(
                "events",
                new String[]{"id", "name", "date", "location"},
                selection,
                selectionArgs,
                null,
                null,
                "date ASC"
        );
    }

    public boolean deleteEvent(long eventId) {

        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                "events",
                "id = ? AND user_id IS NULL",
                new String[]{String.valueOf(eventId)}
        );

        return rowsDeleted > 0;
    }

    public long createUser(String username, String password) {

        // Generate a unique salt
        String salt = PasswordUtils.generateSalt();

        // Hash the user's password
        String passwordHash =
                PasswordUtils.hashPassword(password, salt);

        // Prepare the database
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("password_hash", passwordHash);
        values.put("password_salt", salt);

        // Save the user without replacing existing accounts
        return db.insertWithOnConflict(
                "users",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public long validateLogin(String username, String password) {

        SQLiteDatabase db = getReadableDatabase();

        // Look for an account with this username
        try (Cursor cursor = db.query(
                "users",
                new String[]{"id", "password", "password_salt"},
                "username = ?",
                new String[]{username},
                null,
                null,
                null
        )) {

            // No matching username was found
            if (!cursor.moveToFirst()) {
                return -1;
            }

            // Retrieve the user's saved information
            long userId = cursor.getLong(
                    cursor.getColumnIndexOrThrow("id")
            );

            String storedHash = cursor.getString(
                    cursor.getColumnIndexOrThrow("password_hash")
            );

            String salt = cursor.getString(
                    cursor.getColumnIndexOrThrow("password_salt")
            );

            // Verify the entered password
            boolean passwordMatches = PasswordUtils.verifyPassword(
                            password,
                            salt,
                            storedHash
            );

            if (passwordMatches) {
                return userId;
            }

            return -1;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
    }
}