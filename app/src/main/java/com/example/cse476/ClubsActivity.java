package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubsActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerClubs;
    private ProgressBar progressBar;
    private Button profileButton;
    private Button logoutButton;

    private ClubAdapter adapter;

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
        setContentView(R.layout.activity_clubs);

        searchEditText = findViewById(R.id.searchEditText);
        recyclerClubs = findViewById(R.id.recyclerClubs);
        progressBar = findViewById(R.id.progressBar);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Profile button: go to ProfileActivity
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Logout button: clear session + go to LoginActivity
        logoutButton.setOnClickListener(v -> {
            // Clear everything we stored for this session, including "remember me"
            getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Toast.makeText(ClubsActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ClubsActivity.this, LoginActivity.class);
            // Clear the back stack so user can't hit Back to return here
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            finish();
        });

        // RecyclerView setup
        adapter = new ClubAdapter(club -> {
            Intent intent = new Intent(ClubsActivity.this, ClubDetailsActivity.class);
            intent.putExtra("CLUB_ID", club.id);
            startActivity(intent);
        });

        recyclerClubs.setLayoutManager(new LinearLayoutManager(this));
        recyclerClubs.setAdapter(adapter);
        recyclerClubs.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );

        // Search filter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.applyFilters(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Load data
        fetchClubs();
    }

    private void fetchClubs() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        SupabaseApi api = ApiClient.get(this);
        api.listClubs("*").enqueue(new Callback<List<Club>>() {
            @Override
            public void onResponse(Call<List<Club>> call, Response<List<Club>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ClubsActivity.this, "Failed to load clubs: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Apply hardcoded locations to clubs that don't have addresses
                List<Club> clubs = response.body();
                for (Club club : clubs) {
                    if ((club.address == null || club.address.trim().isEmpty()) 
                            && club.slug != null 
                            && HARDCODED_LOCATIONS.containsKey(club.slug)) {
                        club.address = HARDCODED_LOCATIONS.get(club.slug);
                    }
                }
                
                adapter.setData(clubs);
                adapter.applyFilters(searchEditText.getText().toString());
            }

            @Override
            public void onFailure(Call<List<Club>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(ClubsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Preserve the search value across rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("searchText", searchEditText.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchEditText.setText(savedInstanceState.getString("searchText", ""));
    }
}
