package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEdit, majorEdit, yearEdit, emailEdit;
    private Button saveBtn, deleteBtn;

    private SupabaseApi api;
    private String userId;
    private String userEmail;   // optional, if you stored it in prefs
    private boolean hasExistingProfile = false;  // set after load

    private RecyclerView recyclerFavorites;
    private ClubAdapter favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind profile fields
        nameEdit   = findViewById(R.id.profileName);
        majorEdit  = findViewById(R.id.profileMajor);
        yearEdit   = findViewById(R.id.profileYear);
        emailEdit  = findViewById(R.id.profileEmail);
        saveBtn    = findViewById(R.id.btnSaveProfile);
        deleteBtn  = findViewById(R.id.btnDeleteProfile);

        // ⭐ Bind RecyclerView
        recyclerFavorites = findViewById(R.id.recyclerFavorites);

        if (recyclerFavorites == null) {
            // This means the view is NOT in activity_profile.xml
            android.util.Log.e("ProfileActivity", "recyclerFavorites is NULL! Check activity_profile.xml layout and ID.");
            Toast.makeText(this, "Favorites list not available (layout issue)", Toast.LENGTH_SHORT).show();
            // Bail early for now so we don’t crash
        } else {
            // Set up adapter only if not null
            favoritesAdapter = new ClubAdapter(club -> {
                Intent intent = new Intent(ProfileActivity.this, ClubDetailsActivity.class);
                intent.putExtra("CLUB_ID", club.id);
                startActivity(intent);
            });

            recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
            recyclerFavorites.setAdapter(favoritesAdapter);
            recyclerFavorites.addItemDecoration(
                    new androidx.recyclerview.widget.DividerItemDecoration(
                            this,
                            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                    )
            );
        }

        // API + user info
        api = ApiClient.get(this);
        var prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        userEmail = prefs.getString("EMAIL", "");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Email is read-only
        emailEdit.setText(userEmail);
        emailEdit.setEnabled(false);

        // Load stuff
        loadProfile();
        if (recyclerFavorites != null) {
            loadFavoriteClubs();
        }

        saveBtn.setOnClickListener(v -> saveProfile());
        deleteBtn.setOnClickListener(v -> deleteProfile());
    }


    private void loadProfile() {
        String idFilter = "eq." + userId;

        api.getProfile(idFilter, "*").enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (!response.isSuccessful()) {
                    showToast("Failed to load profile (" + response.code() + ")");
                    return;
                }

                List<Profile> body = response.body();
                if (body == null || body.isEmpty()) {
                    hasExistingProfile = false;
                    showToast("No profile found yet. Fill it in and save!");
                    return;
                }

                hasExistingProfile = true;
                Profile p = body.get(0);

                runOnUiThread(() -> {
                    // Always show the auth email we already know
                    emailEdit.setText(userEmail);

                    nameEdit.setText(p.name != null ? p.name : "");
                    majorEdit.setText(p.major != null ? p.major : "");
                    yearEdit.setText(p.year != null ? p.year : "");
                });
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                showToast("Failed to load profile: " + t.getMessage());
            }
        });
    }

    private void saveProfile() {
        String name  = nameEdit.getText().toString().trim();
        String major = majorEdit.getText().toString().trim();
        String year  = yearEdit.getText().toString().trim();
        String email = userEmail;

        if (name.isEmpty() && major.isEmpty() && year.isEmpty()) {
            Toast.makeText(this, "Please fill at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        Profile p = new Profile(userId, email, name, major, year);

        if (hasExistingProfile) {
            // Update (PATCH)
            String idFilter = "eq." + userId;
            api.updateProfile(idFilter, p).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = "Save error: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    msg += " - " + response.errorBody().string();
                                }
                            } catch (IOException ignored) {}
                            Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    showToast("Update failed: " + t.getMessage());
                }
            });
        } else {
            // Insert (POST)
            api.insertProfile(p).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            hasExistingProfile = true;
                            Toast.makeText(ProfileActivity.this, "Profile created!", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = "Insert error: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    msg += " - " + response.errorBody().string();
                                }
                            } catch (IOException ignored) {}
                            Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    showToast("Insert failed: " + t.getMessage());
                }
            });
        }
    }

    private void deleteProfile() {
        String idFilter = "eq." + userId;

        api.deleteProfile(idFilter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        hasExistingProfile = false;
                        nameEdit.setText("");
                        majorEdit.setText("");
                        yearEdit.setText("");
                        Toast.makeText(ProfileActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this,
                                "Delete error: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("Delete failed: " + t.getMessage());
            }
        });
    }
    private void loadFavoriteClubs() {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        String userFilter = "eq." + userId;
        String select = "*,favorites!inner(user_id)";

        api.getFavoriteClubs(select, userFilter).enqueue(new Callback<List<Club>>() {
            @Override
            public void onResponse(Call<List<Club>> call, Response<List<Club>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showToast("Failed to load favorites (" + response.code() + ")");
                    return;
                }

                List<Club> favClubs = response.body();
                runOnUiThread(() -> {
                    favoritesAdapter.setData(favClubs);
                    favoritesAdapter.applyFilters("");  // no search filter here
                    if (favClubs.isEmpty()) {
                        showToast("You haven't favorited any clubs yet.");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Club>> call, Throwable t) {
                showToast("Failed to load favorites: " + t.getMessage());
            }
        });
    }

    private void showToast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show()
        );
    }


}
