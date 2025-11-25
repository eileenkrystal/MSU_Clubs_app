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

import okhttp3.*;


public class ProfileActivity extends AppCompatActivity {
    private EditText nameEdit, majorEdit, yearEdit, emailEdit;
    private Button saveBtn, deleteBtn;

    private final OkHttpClient client = new OkHttpClient();
    private String token;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameEdit = findViewById(R.id.profileName);
        majorEdit = findViewById(R.id.profileMajor);
        yearEdit = findViewById(R.id.profileYear);
        emailEdit = findViewById(R.id.profileEmail);
        saveBtn = findViewById(R.id.btnSaveProfile);
        deleteBtn = findViewById(R.id.btnDeleteProfile);

        // Load JWT + user ID
        token = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("JWT", null);
        userId = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("USER_ID", null);

        loadProfile();

        saveBtn.setOnClickListener(v -> updateProfile());
        deleteBtn.setOnClickListener(v -> deleteProfile());
    }

    private void loadProfile() {
        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Load error", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();

                try {
                    JSONArray arr = new JSONArray(res);
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

        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        String jsonBody = "{"
                + "\"email\":\"" + emailEdit.getText() + "\","
                + "\"name\":\"" + nameEdit.getText() + "\","
                + "\"major\":\"" + majorEdit.getText() + "\","
                + "\"year\":\"" + yearEdit.getText() + "\""
                + "}";

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void deleteProfile() {

        String url = Config.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Delete failed", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Profile deleted!", Toast.LENGTH_SHORT).show();
                    // Log out user and go back to login
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE).edit().clear().apply();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    finish();
                });
            }
        });
    }
}