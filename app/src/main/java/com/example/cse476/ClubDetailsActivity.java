package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

// THIRD ACTIVITY - shows detailed club information
public class ClubDetailsActivity extends AppCompatActivity {

    // These variables hold references to UI components
    private CheckBox favoriteCheckBox;
    private Button directionsButton;
    private SwitchCompat reminderSwitch;
    private TextView clubNameTextView;
    private TextView meetingTimeTextView;
    private TextView locationTextView;

    // onCreate is called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // connects Java code to the XML layout file
        setContentView(R.layout.activity_club_details);

        // Initialize views - connect Java variables to XML elements
        favoriteCheckBox = findViewById(R.id.favoriteCheckBox);
        directionsButton = findViewById(R.id.directionsButton);
        reminderSwitch = findViewById(R.id.reminderSwitch);
        clubNameTextView = findViewById(R.id.clubNameTextView);
        meetingTimeTextView = findViewById(R.id.meetingTimeTextView);
        locationTextView = findViewById(R.id.locationTextView);

        // Set actual MSU club data using string resources
        clubNameTextView.setText(R.string.wic_club_name);
        meetingTimeTextView.setText(R.string.meeting_time);
        locationTextView.setText(R.string.location);

        // Set up directions button click listener
        directionsButton.setOnClickListener(v -> {
            Toast.makeText(this, R.string.opening_directions, Toast.LENGTH_SHORT).show();
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
    }

    // state preservation
    // This saves data when screen rotates or app goes to background
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the favorite checkbox state AND reminder switch state
        outState.putBoolean("isFavorite", favoriteCheckBox.isChecked());
        outState.putBoolean("reminderOn", reminderSwitch.isChecked());
    }

    // This restores data when screen rotates back
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the favorite checkbox state AND reminder switch state
        favoriteCheckBox.setChecked(savedInstanceState.getBoolean("isFavorite", false));
        reminderSwitch.setChecked(savedInstanceState.getBoolean("reminderOn", false));
    }
}