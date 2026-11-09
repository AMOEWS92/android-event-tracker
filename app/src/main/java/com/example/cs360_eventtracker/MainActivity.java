package com.example.cs360_eventtracker;

import android.os.Bundle;

import android.content.Intent;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        EditText usernameField = findViewById(R.id.editUsername);
        EditText passwordField = findViewById(R.id.editPassword);

        Button createAccountButton =
                findViewById(R.id.buttonCreateAccount);

        createAccountButton.setOnClickListener(v -> {

                    String username = usernameField.getText()
                            .toString().trim();

                    String password = passwordField.getText()
                            .toString();

                    // Validate username
                    if (username.length() < 3) {
                        usernameField.setError(
                                "Username must be at least 3 characters"
                        );
                        return;
                    }

                    // Validate the password
                    if (password.length() < 8) {
                        passwordField.setError(
                                "Password must be at least 8 characters"
                        );
                        return;
                    }

                    createAccountButton.setEnabled(false);

                    // Run password hashing and database work on a background thread
                    new Thread(() -> {

                        long newUserId;

                        try {
                            newUserId = dbHelper.createUser(
                                    username, password
                            );
                        } catch (Exception e) {
                            android.util.Log.e(
                                    "MainActivity",
                                    "Account creation failed",
                                    e
                            );
                            newUserId = -1;
                        }

                        final long createdUserId = newUserId;

                        runOnUiThread(() -> {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            createAccountButton.setEnabled(true);

                            if (createdUserId != -1) {

                                Toast.makeText(this,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT).show();

                                passwordField.setText("");
                            } else {

                                Toast.makeText(this,
                                        "Unable to create account. Username may already exist.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                    }).start();

                });

            Button loginButton = findViewById(R.id.buttonLogin);

            loginButton.setOnClickListener(v -> {

                String username = usernameField.getText()
                        .toString().trim();

                String password = passwordField.getText()
                        .toString();

                // Check that both fields contain information
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this,
                            "Please enter your username and password.",
                            Toast.LENGTH_SHORT).show();

                    return;
                }

                loginButton.setEnabled(false);

                // Validate credentials on a background thread
                new Thread(() -> {

                    long userId;

                    try {
                        userId = dbHelper.validateLogin(
                                username, password
                        );
                    } catch (Exception e) {
                        android.util.Log.e(
                                "MainActivity",
                                "Login failed",
                                e
                        );
                        userId = -1;
                    }

                    final long validatedUserId = userId;

                    runOnUiThread(() -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loginButton.setEnabled(true);

                        if (validatedUserId != -1) {

                            Toast.makeText(this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Open Events and pass the user's ID
                            Intent intent = new Intent(
                                    MainActivity.this,
                                    EventListActivity.class
                            );

                            intent.putExtra("USER_ID", validatedUserId);

                            startActivity(intent);

                        } else {

                            Toast.makeText(this,
                                    "Invalid username or password.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });

        Button continueButton = findViewById(R.id.buttonGoToEvents);

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EventListActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
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
        });
    }
}