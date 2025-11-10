package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

// SECOND ACTIVITY - shows list of clubs
public class ClubsActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button sampleClubButton;
    private CheckBox stemFilterCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);

        // Initialize UI components
        searchEditText = findViewById(R.id.searchEditText);
        sampleClubButton = findViewById(R.id.sampleClubButton);
        stemFilterCheckBox = findViewById(R.id.stemFilterCheckBox);

        // Set actual club name
        sampleClubButton.setText(R.string.wic_club_name);

        // UPDATED: When club button is clicked, go to Club Details activity
        sampleClubButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsActivity.this, ClubDetailsActivity.class);
            // NEW: Pass the club location to the details activity
            intent.putExtra("CLUB_LOCATION", getString(R.string.location));
            startActivity(intent);
        });
    }

    // Save state when screen rotates (unchanged)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("searchText", searchEditText.getText().toString());
        outState.putBoolean("stemFilter", stemFilterCheckBox.isChecked());
    }

    // Restore state when screen rotates back (unchanged)
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchEditText.setText(savedInstanceState.getString("searchText", ""));
        stemFilterCheckBox.setChecked(savedInstanceState.getBoolean("stemFilter", false));
    }
}