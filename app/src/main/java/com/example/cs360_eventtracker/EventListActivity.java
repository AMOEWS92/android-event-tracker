
package com.example.cs360_eventtracker;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EventListActivity extends AppCompatActivity {

    private Long currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_list);

        // Determine whether the user is logged in or a guest
        long incomingUserId = getIntent().getLongExtra("USER_ID", -1L);

        if (incomingUserId > 0) {
            currentUserId = incomingUserId;
        } else {
            currentUserId = null;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        EditText eventName = findViewById(R.id.editEventName);
        EditText eventDate = findViewById(R.id.editEventDate);
        EditText eventTime = findViewById(R.id.editEventTime);
        EditText eventLocation = findViewById(R.id.editEventLocation);
        EditText eventDescription = findViewById(R.id.editEventDescription);


        Button addEventButton = findViewById(R.id.buttonAddEvent);

        // Handle adding a new event
        addEventButton.setOnClickListener(v -> {

            String name = eventName.getText().toString().trim();
            String date = eventDate.getText().toString().trim();
            String time = eventTime.getText().toString().trim();
            String location = eventLocation.getText().toString().trim();
            String description = eventDescription.getText().toString().trim();


            // Validate required fields
            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(this,
                        "Please enter an event name and date.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate date format
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                eventDate.setError("Use YYYY-MM-DD");
                return;
            }

            // Validate event time if provided
            if (!time.isEmpty()) {
                try {
                    LocalTime.parse(
                            time,
                            DateTimeFormatter.ofPattern("HH:mm")
                    );
                } catch (DateTimeParseException e) {
                    eventTime.setError("Use HH:mm, such as 14:30");
                    return;
                }
            }

            // Save event to the current user's collection

            long result = dbHelper.addEvent(
                    currentUserId,
                    name,
                    date,
                    time,
                    location,
                    description
            );

            if (result != -1) {
                Toast.makeText(this,
                        "Event saved successfully!",
                        Toast.LENGTH_SHORT).show();

                eventName.setText("");
                eventDate.setText("");
                eventTime.setText("");
                eventLocation.setText("");
                eventDescription.setText("");

                refreshEventTable(dbHelper);

            } else {
                Toast.makeText(this,
                        "Failed to save event.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Android system bar spacing
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.eventListRoot), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Load existing events when the screen opens
        refreshEventTable(dbHelper);
    }

    // Retrieve and display events from SQLite
    private void refreshEventTable(DatabaseHelper dbHelper) {

        TableLayout table = findViewById(R.id.tableEvents);

        // Remove existing event rows but keep the header
        int eventRowCount = table.getChildCount() - 1;

        if (eventRowCount > 0) {
            table.removeViews(1, eventRowCount);
        }

        // Retrieve events for the logged-in user or guest
        try (Cursor cursor = dbHelper.getEvents(currentUserId)) {

            while (cursor.moveToNext()) {

                long eventId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                String date = cursor.getString(
                        cursor.getColumnIndexOrThrow("date")
                );

                String location = cursor.getString(
                        cursor.getColumnIndexOrThrow("location")
                );


                String time = cursor.getString(
                        cursor.getColumnIndexOrThrow("time")
                );

                String description = cursor.getString(
                        cursor.getColumnIndexOrThrow("description")
                );


                // Display the description underneath the event name
                String eventDisplay = name;

                if (description != null && !description.isEmpty()) {
                    eventDisplay += "\n" + description;
                }

                // Display the time underneath the event date
                String dateDisplay = date;

                if (time != null && !time.isEmpty()) {
                    dateDisplay += "\n" + time;
                }


                TableRow row = new TableRow(this);

                row.addView(createEventCell(eventDisplay));
                row.addView(createEventCell(dateDisplay));
                row.addView(createEventCell(location));

                // Create Edit button
                Button editButton = new Button(this);
                editButton.setText("Edit");
                editButton.setContentDescription("Edit " + name);

                editButton.setOnClickListener(v -> {
                    showEditEventDialog(
                            eventId,
                            name,
                            date,
                            location,
                            dbHelper
                    );
                });

                // Create Delete button
                Button deleteButton = new Button(this);
                deleteButton.setText("X");
                deleteButton.setContentDescription("Delete " + name);

                deleteButton.setOnClickListener(v -> {

                    new AlertDialog.Builder(this)
                            .setTitle("Delete Event")
                            .setMessage(
                                    "Are you sure you want to delete "
                                            + name + "?"
                            )
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", (dialog, which) -> {

                                boolean deleted = dbHelper.deleteEvent(
                                        eventId,
                                        currentUserId
                                );

                                if (deleted) {
                                    Toast.makeText(this,
                                            "Event deleted successfully!",
                                            Toast.LENGTH_SHORT).show();

                                    refreshEventTable(dbHelper);

                                } else {
                                    Toast.makeText(this,
                                            "Failed to delete event.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                });

                // Place Edit and Delete in the Actions column
                LinearLayout actionsLayout = new LinearLayout(this);
                actionsLayout.setOrientation(LinearLayout.VERTICAL);

                actionsLayout.addView(editButton);
                actionsLayout.addView(deleteButton);

                row.addView(actionsLayout);

                // Display the completed row
                table.addView(row);
            }
        }
    }

    // Create a formatted cell for the event table
    private TextView createEventCell(String value) {

        TextView cell = new TextView(this);

        cell.setText(value == null ? "" : value);

        cell.setLayoutParams(new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                1f
        ));

        int padding = (int) (
                8 * getResources().getDisplayMetrics().density
        );

        cell.setPadding(padding, padding, padding, padding);

        return cell;
    }

    // Open a dialog to edit an existing event
    private void showEditEventDialog(
            long eventId,
            String name,
            String date,
            String location,
            DatabaseHelper dbHelper) {

        // Create a vertical layout for the input fields
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (
                16 * getResources().getDisplayMetrics().density
        );

        layout.setPadding(padding, padding, padding, padding);

        // Event name field
        EditText nameField = new EditText(this);
        nameField.setHint("Event name");
        nameField.setText(name);
        layout.addView(nameField);

        // Event date field
        EditText dateField = new EditText(this);
        dateField.setHint("YYYY-MM-DD");
        dateField.setText(date);
        layout.addView(dateField);

        // Event location field
        EditText locationField = new EditText(this);
        locationField.setHint("Location");
        locationField.setText(location);
        layout.addView(locationField);

        // Create the editing dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Event")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        // Handle Save button clicks
        dialog.setOnShowListener(ignored -> {

            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String updatedName = nameField.getText()
                                .toString().trim();

                        String updatedDate = dateField.getText()
                                .toString().trim();

                        String updatedLocation = locationField.getText()
                                .toString().trim();

                        // Validate event name
                        if (updatedName.isEmpty()) {
                            nameField.setError("Event name is required");
                            return;
                        }

                        // Validate event date
                        try {
                            LocalDate.parse(updatedDate);
                        } catch (DateTimeParseException e) {
                            dateField.setError("Use YYYY-MM-DD");
                            return;
                        }

                        // Update the event in SQLite
                        boolean updated = dbHelper.updateEvent(
                                eventId,
                                currentUserId,
                                updatedName,
                                updatedDate,
                                updatedLocation
                        );

                        if (updated) {

                            Toast.makeText(this,
                                    "Event updated successfully!",
                                    Toast.LENGTH_SHORT).show();

                            // Reload the event list
                            refreshEventTable(dbHelper);

                            // Close the dialog
                            dialog.dismiss();

                        } else {

                            Toast.makeText(this,
                                    "Failed to update event.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Display the editing dialog
        dialog.show();
    }
}