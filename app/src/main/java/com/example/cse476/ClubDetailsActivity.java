package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubDetailsActivity extends AppCompatActivity {

    private LocationHelper locationHelper;
    private String clubLocation; // used by directions button

    // New: track IDs
    private String clubId;
    private String userId;

    // Views
    private TextView clubNameTextView;
    private TextView meetingTimeTextView;
    private TextView locationTextView;
    private TextView clubDescriptionTextView;
    private CheckBox favoriteCheckBox;
    private SwitchCompat reminderSwitch;
    private Button directionsButton;

    // To avoid firing network calls when we just setChecked programmatically
    private boolean isUpdatingFavoriteUi = false;

    // Hardcoded locations for demo purposes (first few clubs alphabetically)
    private static final Map<String, String> HARDCODED_LOCATIONS = new HashMap<>();
    static {
        // Map club slug -> MSU campus location
        HARDCODED_LOCATIONS.put("021", "Engineering Building, 428 S Shaw Ln, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("180", "Minskoff Pavilion, 651 N Shaw Ln, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("4michmsu", "Student Services Building, 556 E Circle Dr, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("aac", "MSU Union, 49 Abbot Rd, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("aafmsu", "Communication Arts Building, 404 Wilson Rd, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("aaig", "Life Sciences Building, 1355 Bogue St, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("aasomsu", "International Center, 427 N Shaw Ln, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("abide", "MSU Union, 49 Abbot Rd, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("absws", "Baker Hall, 655 Auditorium Rd, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("abwd", "Life Sciences Building, 1355 Bogue St, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("acm", "Engineering Building, 428 S Shaw Ln, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("acs", "International Center, 427 N Shaw Ln, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("actuarial", "Wells Hall, 619 Red Cedar Rd, East Lansing, MI 48824");
        HARDCODED_LOCATIONS.put("aero", "Bessey Hall, 434 Farm Ln, East Lansing, MI 48824");
    }

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

        // IDs
        clubId = getIntent().getStringExtra("CLUB_ID");
        userId = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("USER_ID", null);

        if (clubId == null || clubId.isEmpty()) {
            bindEmpty("No club id provided");
        } else {
            fetchClubById(clubId);
        }

        // Directions button
        directionsButton.setOnClickListener(v -> handleGetDirections());

        // Reminder switch (local only for now)
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, R.string.reminder_set, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
            }
        });

        // Favorite checkbox
        favoriteCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // If we're just syncing UI from network, don't call API again
            if (isUpdatingFavoriteUi) return;

            if (userId == null) {
                Toast.makeText(this, "Please log in again to use favorites.", Toast.LENGTH_SHORT).show();
                // Reset UI to unchecked if we can't favorite
                isUpdatingFavoriteUi = true;
                favoriteCheckBox.setChecked(false);
                isUpdatingFavoriteUi = false;
                return;
            }

            if (clubId == null || clubId.isEmpty()) {
                Toast.makeText(this, "Club ID missing, cannot update favorites.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isChecked) {
                addFavorite();
            } else {
                removeFavorite();
            }
        });

        // Restore UI state (reminder only; favorite now comes from backend)
        if (savedInstanceState != null) {
            reminderSwitch.setChecked(savedInstanceState.getBoolean("reminderOn", false));
        }

        // After listeners are set, check favorite status if we have both IDs
        if (clubId != null && !clubId.isEmpty() && userId != null) {
            checkIfFavorite();
        }
    }

    // ---------------- CLUB DATA ----------------

    private void fetchClubById(String id) {
        SupabaseApi api = ApiClient.get(this);
        api.getClubById("eq." + id, "*").enqueue(new Callback<List<Club>>() {
            @Override
            public void onResponse(Call<List<Club>> call, Response<List<Club>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    bindEmpty("Club not found (" + response.code() + ")");
                    return;
                }
                Club club = response.body().get(0);
                
                // Apply hardcoded location if club doesn't have one
                if ((club.address == null || club.address.trim().isEmpty()) 
                        && club.slug != null 
                        && HARDCODED_LOCATIONS.containsKey(club.slug)) {
                    club.address = HARDCODED_LOCATIONS.get(club.slug);
                }
                
                bindClub(club);
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

    // ---------------- FAVORITES LOGIC ----------------

    private void checkIfFavorite() {
        SupabaseApi api = ApiClient.get(this);
        String userFilter = "eq." + userId;
        String clubFilter = "eq." + clubId;

        api.getFavorite(userFilter, clubFilter, "*").enqueue(new Callback<List<Favorite>>() {
            @Override
            public void onResponse(Call<List<Favorite>> call, Response<List<Favorite>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    // If it fails, just leave unchecked silently
                    return;
                }

                boolean isFav = !response.body().isEmpty();

                isUpdatingFavoriteUi = true;
                favoriteCheckBox.setChecked(isFav);
                isUpdatingFavoriteUi = false;
            }

            @Override
            public void onFailure(Call<List<Favorite>> call, Throwable t) {
                // Ignore on failure; user can still try to toggle
            }
        });
    }

    private void addFavorite() {
        SupabaseApi api = ApiClient.get(this);
        Favorite fav = new Favorite(userId, clubId);

        api.addFavorite(fav).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    // Revert UI if backend failed
                    isUpdatingFavoriteUi = true;
                    favoriteCheckBox.setChecked(false);
                    isUpdatingFavoriteUi = false;
                    Toast.makeText(ClubDetailsActivity.this, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ClubDetailsActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isUpdatingFavoriteUi = true;
                favoriteCheckBox.setChecked(false);
                isUpdatingFavoriteUi = false;
                Toast.makeText(ClubDetailsActivity.this, "Error adding favorite", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFavorite() {
        SupabaseApi api = ApiClient.get(this);
        String userFilter = "eq." + userId;
        String clubFilter = "eq." + clubId;

        api.deleteFavorite(userFilter, clubFilter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    // Revert UI if backend failed
                    isUpdatingFavoriteUi = true;
                    favoriteCheckBox.setChecked(true);
                    isUpdatingFavoriteUi = false;
                    Toast.makeText(ClubDetailsActivity.this, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ClubDetailsActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isUpdatingFavoriteUi = true;
                favoriteCheckBox.setChecked(true);
                isUpdatingFavoriteUi = false;
                Toast.makeText(ClubDetailsActivity.this, "Error removing favorite", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- DIRECTIONS / PERMISSIONS ----------------

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
        // Favorite state now lives in backend; only preserve reminder toggle
        outState.putBoolean("reminderOn", reminderSwitch.isChecked());
    }
}
