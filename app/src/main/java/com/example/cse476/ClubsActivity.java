package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

        Button profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(ClubsActivity.this, ProfileActivity.class));
        });

        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener(v -> deleteAccount());

        // Set actual club name
        sampleClubButton.setText(R.string.wic_club_name);

        // UPDATED: When club button is clicked, go to Club Details activity
        sampleClubButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsActivity.this, ClubDetailsActivity.class);
            intent.putExtra("CLUB_LOCATION", getString(R.string.location));
            startActivity(intent);
        });
    }

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

    private void deleteAccount() {
        String token = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .getString("JWT", null);

        if (token == null) {
            runOnUiThread(() ->
                    Toast.makeText(ClubsActivity.this, "Not authenticated", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String url = Config.SUPABASE_URL + "/auth/v1/user";

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ClubsActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    runOnUiThread(() -> {
                        Toast.makeText(ClubsActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ClubsActivity.this, LoginActivity.class));
                        finish();
                    });

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ClubsActivity.this, "Delete failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}
