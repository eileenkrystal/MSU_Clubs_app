package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEdit, majorEdit, yearEdit, emailEdit;
    private Button saveBtn, deleteBtn;

    private final OkHttpClient client = new OkHttpClient();
    private String token;
    private String userId;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Hook up views
        nameEdit = findViewById(R.id.profileName);
        majorEdit = findViewById(R.id.profileMajor);
        yearEdit = findViewById(R.id.profileYear);
        emailEdit = findViewById(R.id.profileEmail);
        saveBtn = findViewById(R.id.btnSaveProfile);
        deleteBtn = findViewById(R.id.btnDeleteProfile);

        // Load JWT + user ID from shared prefs
        token = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("JWT", null);
        userId = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("USER_ID", null);

        if (token == null || userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        loadProfile();

        saveBtn.setOnClickListener(v -> updateProfile());
        deleteBtn.setOnClickListener(v -> deleteProfile());
    }

    private void loadProfile() {
        // Adjust "profiles" to whatever view/table you're actually using
        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(ProfileActivity.this, "Load error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                try {
                    JSONArray arr = new JSONArray(res);
                    if (arr.length() == 0) {
                        // No profile row yet – just show email if you have it, leave others blank
                        runOnUiThread(() ->
                                Toast.makeText(ProfileActivity.this, "No profile found yet", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    JSONObject obj = arr.getJSONObject(0);

                    runOnUiThread(() -> {
                        emailEdit.setText(obj.optString("email", ""));
                        nameEdit.setText(obj.optString("name", ""));
                        majorEdit.setText(obj.optString("major", ""));
                        yearEdit.setText(obj.optString("year", ""));
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(ProfileActivity.this, "Parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateProfile() {
        String name = nameEdit.getText().toString().trim();
        String major = majorEdit.getText().toString().trim();
        String year = yearEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim(); // read-only in UI, but we still send if your table uses it

        if (name.isEmpty() && major.isEmpty() && year.isEmpty()) {
            Toast.makeText(this, "Please fill out at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        try {
            JSONObject bodyJson = new JSONObject();
            // include fields that make sense for your "profiles" row
            bodyJson.put("email", email);
            bodyJson.put("name", name);
            bodyJson.put("major", major);
            bodyJson.put("year", year);

            RequestBody body = RequestBody.create(bodyJson.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Save error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteProfile() {
        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Delete failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile deleted!", Toast.LENGTH_SHORT).show();
                        // Clear local session + go back to login
                        getSharedPreferences("APP_PREFS", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Delete error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
