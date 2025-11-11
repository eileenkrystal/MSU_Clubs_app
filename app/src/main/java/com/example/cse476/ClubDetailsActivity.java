package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.pm.PackageManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubDetailsActivity extends AppCompatActivity {

    private LocationHelper locationHelper;
    private String clubLocation; // used by directions button

    // Views
    private TextView clubNameTextView;
    private TextView meetingTimeTextView;
    private TextView locationTextView;
    private TextView clubDescriptionTextView;
    private CheckBox favoriteCheckBox;
    private SwitchCompat reminderSwitch;
    private Button directionsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_details);

        locationHelper = new LocationHelper(this);

        // Bind views
        favoriteCheckBox = findViewById(R.id.favoriteCheckBox);
        directionsButton = findViewById(R.id.directionsButton);
        reminderSwitch = findViewById(R.id.reminderSwitch);
        clubNameTextView = findViewById(R.id.clubNameTextView);
        meetingTimeTextView = findViewById(R.id.meetingTimeTextView);
        locationTextView = findViewById(R.id.locationTextView);
        clubDescriptionTextView = findViewById(R.id.clubDescriptionTextView);

        // Set neutral placeholders (XML uses tools:text only)
        clubNameTextView.setText("Loading…");
        locationTextView.setText("");
        if (clubDescriptionTextView != null) clubDescriptionTextView.setText("");

        // “Reset every time”: fetch fresh by CLUB_ID
        String clubId = getIntent().getStringExtra("CLUB_ID");
        if (clubId == null || clubId.isEmpty()) {
            bindEmpty("No club id provided");
        } else {
            fetchClubById(clubId);
        }

        // Keep your listeners
        directionsButton.setOnClickListener(v -> handleGetDirections());
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, R.string.reminder_set, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
            }
        });

        // Restore UI state (favorites/reminder) if rotating
        if (savedInstanceState != null) {
            favoriteCheckBox.setChecked(savedInstanceState.getBoolean("isFavorite", false));
            reminderSwitch.setChecked(savedInstanceState.getBoolean("reminderOn", false));
        }
    }

    private void fetchClubById(String id) {
        SupabaseApi api = ApiClient.get(this);
        api.getClubById("eq." + id, "*").enqueue(new Callback<List<Club>>() {
            @Override
            public void onResponse(Call<List<Club>> call, Response<List<Club>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    bindEmpty("Club not found (" + response.code() + ")");
                    return;
                }
                bindClub(response.body().get(0));
            }

            @Override
            public void onFailure(Call<List<Club>> call, Throwable t) {
                bindEmpty("Error: " + t.getMessage());
            }
        });
    }

    private void bindClub(Club c) {
        // Name
        String name = safe(c.name, "Club");
        clubNameTextView.setText(name);

        // Location/address -> also used for directions
        String address = safe(c.address, "");
        locationTextView.setText(address);
        clubLocation = address;

        // Optional description
        if (clubDescriptionTextView != null) {
            String desc = safe(c.description, "");
            clubDescriptionTextView.setText(desc);
        }

        // Meeting time (you can map a column later; keep placeholder empty for now)
        meetingTimeTextView.setText("");
    }

    private void bindEmpty(String reason) {
        clubNameTextView.setText("Club");
        locationTextView.setText("");
        if (clubDescriptionTextView != null) clubDescriptionTextView.setText(reason);
        clubLocation = null;
    }

    private String safe(String s, String fallback) {
        if (s == null) return fallback;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
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
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                handleGetDirections();
            } else {
                Toast.makeText(this, "Location permission is required for directions", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isFavorite", favoriteCheckBox.isChecked());
        outState.putBoolean("reminderOn", reminderSwitch.isChecked());
    }
}
