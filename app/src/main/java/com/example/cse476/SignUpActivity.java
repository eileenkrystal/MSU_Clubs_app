package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEdit, passwordEdit;
    private Button signupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        emailEdit = findViewById(R.id.signupEmail);
        passwordEdit = findViewById(R.id.signupPassword);
        signupBtn = findViewById(R.id.createAccountButton);

        signupBtn.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        String email = emailEdit.getText().toString().trim();
        String pwd = passwordEdit.getText().toString().trim();

        if (email.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Config.SUPABASE_URL + "/auth/v1/signup";

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String bodyString = "{\"email\":\"" + email + "\",\"password\":\"" + pwd + "\"}";
        RequestBody body = RequestBody.create(JSON, bodyString);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", Config.SUPABASE_ANNON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SignUpActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(SignUpActivity.this, "Signup failed", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                try {
                    JSONObject json = new JSONObject(res);

                    // Supabase signup usually returns { user: {...}, ... }
                    if (!json.has("user")) {
                        runOnUiThread(() ->
                                Toast.makeText(SignUpActivity.this,
                                        "Signup succeeded but user info missing.",
                                        Toast.LENGTH_SHORT).show()
                        );
                        finish();
                        return;
                    }

                    JSONObject userJson = json.getJSONObject("user");
                    String userId = userJson.optString("id", null);
                    String userEmail = userJson.optString("email", email);

                    if (userId == null || userId.isEmpty()) {
                        runOnUiThread(() ->
                                Toast.makeText(SignUpActivity.this,
                                        "Signup succeeded but user id missing.",
                                        Toast.LENGTH_SHORT).show()
                        );
                        finish();
                        return;
                    }

                    // ✅ Insert into custom `users` table
                    createUserRowInSupabase(userId, userEmail);

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(SignUpActivity.this, "Signup parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void createUserRowInSupabase(String userId, String email) {
        SupabaseApi api = ApiClient.get(SignUpActivity.this);
        UserRow row = new UserRow(userId, email, null); // display_name null for now

        api.createUser(row).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call,
                                   retrofit2.Response<Void> response) {

                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this,
                                "Account created, but user profile save failed (" + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Account created! Check your email to verify.",
                                Toast.LENGTH_LONG).show();
                    }
                    // Go back to Login
                    finish();
                });
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpActivity.this,
                            "Account created, but user profile save error.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
