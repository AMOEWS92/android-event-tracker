
package com.example.cs360_eventtracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.EditText;
import android.widget.Toast;

public class SmsSettingsActivity extends AppCompatActivity {

    private Switch smsSwitch;
    private Button permissionButton;
    private TextView smsStatus;
    private SharedPreferences preferences;
    private EditText phoneNumberField;
    private Button savePhoneButton;

    // Handle the user's permission response
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                        saveSmsPreference(isGranted);
                        refreshSmsStatus();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sms_settings);

        // Connect the interface controls
        smsSwitch = findViewById(R.id.switchSmsEnable);
        permissionButton = findViewById(R.id.buttonCheckPermission);
        smsStatus = findViewById(R.id.textSmsStatus);
        phoneNumberField = findViewById(R.id.editPhoneNumber);
        savePhoneButton = findViewById(R.id.buttonSavePhoneNumber);


        // Store SMS preferences separately for each account
        long userId = getIntent().getLongExtra("USER_ID", -1L);

        String preferencesName;

        if (userId > 0) {
            preferencesName = "event_tracker_settings_user_" + userId;
        } else {
            preferencesName = "event_tracker_settings_guest";
        }

        preferences = getSharedPreferences(
                preferencesName,
                MODE_PRIVATE
        );

        // Load the previously saved phone number
        phoneNumberField.setText(
                preferences.getString("phone_number", "")
        );


        // Handle saving the phone number
        savePhoneButton.setOnClickListener(v -> {

            String phoneNumber = phoneNumberField.getText()
                    .toString().trim();

            // Remove common phone number formatting
            phoneNumber = phoneNumber.replaceAll("[\\s()-]", "");

            // Validate the phone number
            if (!phoneNumber.matches("\\+?[0-9]{10,15}")) {
                phoneNumberField.setError("Enter a valid phone number");
                return;
            }

            // Save the phone number locally
            preferences.edit()
                    .putString("phone_number", phoneNumber)
                    .apply();

            Toast.makeText(this,
                    "Phone number saved successfully!",
                    Toast.LENGTH_SHORT).show();

            refreshSmsStatus();
        });



        // Handle permission button clicks
        permissionButton.setOnClickListener(v -> {
            permissionLauncher.launch(
                    Manifest.permission.SEND_SMS
            );
        });

        // Handle SMS toggle changes
        smsSwitch.setOnClickListener(v -> {

            if (smsSwitch.isChecked()) {

                if (hasSmsPermission()) {
                    saveSmsPreference(true);
                } else {
                    permissionLauncher.launch(
                            Manifest.permission.SEND_SMS
                    );
                }

            } else {
                saveSmsPreference(false);
            }

            refreshSmsStatus();
        });

        // Handle Android system bar spacing
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.smsRoot), (v, insets) -> {

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

        refreshSmsStatus();
    }

    // Refresh settings when returning to this screen
    @Override
    protected void onResume() {
        super.onResume();
        refreshSmsStatus();
    }

    // Check whether Android has granted SMS permission
    private boolean hasSmsPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // Save the user's SMS preference
    private void saveSmsPreference(boolean enabled) {

        preferences.edit()
                .putBoolean("sms_enabled", enabled)
                .apply();
    }

    // Update the interface based on permission and preference
    private void refreshSmsStatus() {

        boolean permissionGranted = hasSmsPermission();

        if (!permissionGranted) {
            saveSmsPreference(false);
        }

        boolean enabled = preferences.getBoolean(
                "sms_enabled", false
        );

        smsSwitch.setChecked(enabled && permissionGranted);

        permissionButton.setEnabled(!permissionGranted);

        if (!permissionGranted) {

            permissionButton.setText("Enable SMS Permissions");

            smsStatus.setText(
                    "SMS permission not granted. "
                            + "You can still create and manage events."
            );


        } else if (enabled) {

            permissionButton.setText("SMS Permission Granted");

            String phoneNumber = preferences.getString(
                    "phone_number", ""
            );

            if (phoneNumber.isEmpty()) {
                smsStatus.setText(
                        "Please save a phone number to receive SMS reminders."
                );
            } else {
                smsStatus.setText(
                        "SMS event reminders are enabled."
                );
            }

        } else {

            permissionButton.setText("SMS Permission Granted");

            smsStatus.setText(
                    "SMS event reminders are turned off."
            );
        }
    }
}