package com.example.cs360_eventtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "event_tracker.db";
    private static final int DATABASE_VERSION = 2;

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
                        "time TEXT, " +
                        "location TEXT, " +
                        "description TEXT, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id) " +
                        "ON DELETE CASCADE)";
        db.execSQL(createEventsTable);
    }


    // Original method: four parameters
    public long addEvent(Long userId, String name,
                         String date, String location) {

        return addEvent(
                userId,
                name,
                date,
                null,
                location,
                null
        );
    }

    // New method: six parameters
    public long addEvent(Long userId, String name,
                         String date, String time,
                         String location, String description) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        if (userId != null) {
            values.put("user_id", userId);
        }

        values.put("name", name);
        values.put("date", date);
        values.put("time", time);
        values.put("location", location);
        values.put("description", description);

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
                new String[]{
                        "id",
                        "name",
                        "date",
                        "time",
                        "location",
                        "description"
                },
                selection,
                selectionArgs,
                null,
                null,
                "date ASC"
        );
    }

    public boolean deleteEvent(long eventId, Long userId) {

        SQLiteDatabase db = getWritableDatabase();

        String whereClause;
        String[] whereArgs;

        if (userId == null) {
            // Delete an event belonging to a guest
            whereClause = "id = ? AND user_id IS NULL";

            whereArgs = new String[]{
                    String.valueOf(eventId)
            };
        } else {
            // Delete an event belonging to the logged-in user
            whereClause = "id = ? AND user_id = ?";

            whereArgs = new String[]{
                    String.valueOf(eventId),
                    String.valueOf(userId)
            };
        }

        int rowsDeleted = db.delete(
                "events",
                whereClause,
                whereArgs
        );

        return rowsDeleted > 0;
    }

    public boolean updateEvent(long eventId, Long userId,
                                String name, String date,
                                String location) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("date", date);
        values.put("location", location);

        String whereClause;
        String[] whereArgs;

        if (userId == null) {

            // Update a guest event
            whereClause = "id = ? AND user_id IS NULL";

            whereArgs = new String[]{
                    String.valueOf(eventId)
            };

        } else {

            // Update an event belonging to a specific user
            whereClause = "id = ? AND user_id = ?";

            whereArgs = new String[]{
                    String.valueOf(eventId),
                    String.valueOf(userId)
            };
        }

        int rowsUpdated = db.update(
                "events",
                values,
                whereClause,
                whereArgs
        );

        return rowsUpdated > 0;
    }

    public boolean updateEvent(long eventId, Long userId,
                               String name, String date,
                               String time, String location,
                               String description) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("date", date);
        values.put("time", time);
        values.put("location", location);
        values.put("description", description);

        String whereClause;
        String[] whereArgs;

        if (userId == null) {

            // Update a guest event
            whereClause = "id = ? AND user_id IS NULL";

            whereArgs = new String[]{
                    String.valueOf(eventId)
            };

        } else {

            // Update an event belonging to a user
            whereClause = "id = ? AND user_id = ?";

            whereArgs = new String[]{
                    String.valueOf(eventId),
                    String.valueOf(userId)
            };
        }

        int rowsUpdated = db.update(
                "events",
                values,
                whereClause,
                whereArgs
        );

        return rowsUpdated > 0;
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
                new String[]{"id", "password_hash", "password_salt"},
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
        if (oldVersion < 2) {

            db.execSQL(
                    "ALTER TABLE events ADD COLUMN time TEXT"
            );

            db.execSQL(
                    "ALTER TABLE events ADD COLUMN description TEXT"
            );
        }
    }
}