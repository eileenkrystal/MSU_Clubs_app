package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.content.pm.PackageManager;

// THIRD ACTIVITY - shows detailed club information
public class ClubDetailsActivity extends AppCompatActivity {

    // Only keep fields that are used across multiple methods
    private LocationHelper locationHelper;
    private String clubLocation;

    // onCreate is called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // connects Java code to the XML layout file
        setContentView(R.layout.activity_club_details);

        // Initialize LocationHelper
        locationHelper = new LocationHelper(this);

        // Get club location from intent or use default
        clubLocation = getIntent().getStringExtra("CLUB_LOCATION");
        if (clubLocation == null) {
            // Fallback to the location from string resources if no intent extra
            clubLocation = getString(R.string.location);
        }

        // Initialize views as LOCAL variables since they're only used in onCreate
        CheckBox favoriteCheckBox = findViewById(R.id.favoriteCheckBox);
        Button directionsButton = findViewById(R.id.directionsButton);
        SwitchCompat reminderSwitch = findViewById(R.id.reminderSwitch);
        TextView clubNameTextView = findViewById(R.id.clubNameTextView);
        TextView meetingTimeTextView = findViewById(R.id.meetingTimeTextView);
        TextView locationTextView = findViewById(R.id.locationTextView);

        // Set actual MSU club data using string resources so theres no hard coded strings
        clubNameTextView.setText(R.string.wic_club_name);
        meetingTimeTextView.setText(R.string.meeting_time);
        locationTextView.setText(R.string.location_display);

        // Set up directions button click listener with location integration
        directionsButton.setOnClickListener(v -> {
            handleGetDirections();
        });

        // Set up reminder switch listener
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // make the reminder one hour before the meeting
                Toast.makeText(this, R.string.reminder_set, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
            }
        });

        // Save state when screen rotates or app goes to background
        // We need to use the local variables here
        if (savedInstanceState != null) {
            // Restore the favorite checkbox state AND reminder switch state
            favoriteCheckBox.setChecked(savedInstanceState.getBoolean("isFavorite", false));
            reminderSwitch.setChecked(savedInstanceState.getBoolean("reminderOn", false));
        }
    }

    private void handleGetDirections() {
        if (clubLocation == null || clubLocation.isEmpty()) {
            Toast.makeText(this, "No location available for this club", Toast.LENGTH_SHORT).show();
            return;
        }

        locationHelper.openDirections(this, clubLocation);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, try getting directions again
                handleGetDirections();
            } else {
                Toast.makeText(this, "Location permission is required for directions", Toast.LENGTH_LONG).show();
            }
        }
    }

    // state preservation
    // This saves data when screen rotates or app goes to background
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }


}