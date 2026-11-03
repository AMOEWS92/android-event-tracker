
package com.example.cs360_eventtracker;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import android.database.Cursor;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class EventListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_list);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Find the input fields and button
        EditText eventName = findViewById(R.id.editEventName);
        EditText eventDate = findViewById(R.id.editEventDate);
        EditText eventLocation = findViewById(R.id.editEventLocation);

        Button addEventButton = findViewById(R.id.buttonAddEvent);

        // Listen for the Add Event button being clicked
        addEventButton.setOnClickListener(v -> {

            String name = eventName.getText().toString().trim();
            String date = eventDate.getText().toString().trim();
            String location = eventLocation.getText().toString().trim();

            // Make sure required fields are filled in
            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(this,
                        "Please enter an event name and date.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Check that the dates uses YYYY-MM-DD
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                eventDate.setError("Use YYYY-MM-DD");
                return;
            }

            // Save the event as a guest event for now
            long result = dbHelper.addEvent(null, name, date, location);

            if (result != -1) {
                Toast.makeText(this,
                        "Event saved successfully!",
                        Toast.LENGTH_SHORT).show();

                eventName.setText("");
                eventDate.setText("");
                eventLocation.setText("");

                refreshEventTable(dbHelper);

            } else {
                Toast.makeText(this,
                        "Failed to save event.",
                        Toast.LENGTH_SHORT).show();
            }
        });

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

        refreshEventTable(dbHelper);
    }

    private void refreshEventTable(DatabaseHelper dbHelper) {
        TableLayout table = findViewById(R.id.tableEvents);

        // Remove old event rows but keep the header
        int eventRowCount = table.getChildCount() - 1;

        if (eventRowCount > 0) {
            table.removeViews(1, eventRowCount);
        }

        // Retrieve saved guest events
        try (Cursor cursor = dbHelper.getEvents(null)) {

            while (cursor.moveToNext()) {

                long eventId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));

                String date = cursor.getString(
                        cursor.getColumnIndexOrThrow("date"));

                String location = cursor.getString(
                        cursor.getColumnIndexOrThrow("location"));

                TableRow row = new TableRow(this);

                row.addView(createEventCell(name));
                row.addView(createEventCell(date));
                row.addView(createEventCell(location));

                Button deleteButton = new Button(this);
                deleteButton.setText("X");
                deleteButton.setContentDescription("Delete" + name);

                deleteButton.setOnClickListener(v ->{

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete Event")
                            .setMessage("Are you sure you want to delete " + name + "?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", (dialog, which) ->{

                                boolean deleted = dbHelper.deleteEvent(eventId);

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

                row.addView(deleteButton);

                table.addView(row);
            }
        }
    }

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
}