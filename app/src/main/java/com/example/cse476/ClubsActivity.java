package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubsActivity extends AppCompatActivity {

    private EditText searchEditText;
    private CheckBox stemFilterCheckBox;
    private RecyclerView recyclerClubs;
    private ProgressBar progressBar;

    private ClubAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);

        searchEditText = findViewById(R.id.searchEditText);
        stemFilterCheckBox = findViewById(R.id.stemFilterCheckBox);
        recyclerClubs = findViewById(R.id.recyclerClubs);
        progressBar = findViewById(R.id.progressBar);

        // RecyclerView setup
        adapter = new ClubAdapter(club -> {
            // Click -> go to details
            Intent intent = new Intent(ClubsActivity.this, ClubDetailsActivity.class);
            intent.putExtra("CLUB_ID", club.id);
            startActivity(intent);
        });

        recyclerClubs.setLayoutManager(new LinearLayoutManager(this));
        recyclerClubs.setAdapter(adapter);
        recyclerClubs.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );

        // Filters
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.applyFilters(s.toString(), stemFilterCheckBox.isChecked());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        stemFilterCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.applyFilters(searchEditText.getText().toString(), isChecked);
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
                adapter.setData(response.body());
                // apply current filters
                adapter.applyFilters(searchEditText.getText().toString(), stemFilterCheckBox.isChecked());
            }

            @Override
            public void onFailure(Call<List<Club>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(ClubsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Preserve the search/filter values across rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("searchText", searchEditText.getText().toString());
        outState.putBoolean("stemFilter", stemFilterCheckBox.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchEditText.setText(savedInstanceState.getString("searchText", ""));
        stemFilterCheckBox.setChecked(savedInstanceState.getBoolean("stemFilter", false));
    }
}
