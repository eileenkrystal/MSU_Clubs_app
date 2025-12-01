package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cse476.Config;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

// FIRST ACTIVITY - handles user login
public class LoginActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    // Variables that hold references to UI components
    private EditText netIdEditText;
    private EditText passwordEditText;
    private CheckBox rememberMeCheckBox;
    private Button loginButton;

    // onCreate is called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views
        netIdEditText = findViewById(R.id.netIdEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);

        // Load prefs
        var prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String existingToken = prefs.getString("JWT", null);
        boolean remember = prefs.getBoolean("REMEMBER_ME", false);
        String savedEmail = prefs.getString("SAVED_EMAIL", "");

        // Prefill email / checkbox
        if (!savedEmail.isEmpty()) {
            netIdEditText.setText(savedEmail);
        }
        rememberMeCheckBox.setChecked(remember);

        // If user wanted to be remembered and we still have a token,
        // skip login screen entirely
        if (remember && existingToken != null) {
            startActivity(new Intent(LoginActivity.this, ClubsActivity.class));
            finish();
            return;
        }

        loginButton.setOnClickListener(v -> attemptLogin());

        Button signupRedirect = findViewById(R.id.signupRedirectButton);
        signupRedirect.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );
    }


    // This method handles the login process
//    private void attemptLogin() {
//        // Get text from input fields
//        String netId = netIdEditText.getText().toString();
//        String password = passwordEditText.getText().toString();
//
//        // create an if statement to check if fields are empty
//        if (netId.isEmpty() || password.isEmpty()) {
//            Toast.makeText(this, R.string.enter_cred, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//
//        // Create an Intent to navigate to ClubsActivity
//        Intent intent = new Intent(LoginActivity.this, ClubsActivity.class);
//        startActivity(intent); // This actually starts the new activity
//    }

    // State Preservation
    // This saves data when screen rotates or app goes to background
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the NetID and checkbox state
        outState.putString("netId", netIdEditText.getText().toString());
        outState.putBoolean("rememberMe", rememberMeCheckBox.isChecked());
    }

    // This restores the data when screen rotates back
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the NetID and checkbox state
        netIdEditText.setText(savedInstanceState.getString("netId", ""));
        rememberMeCheckBox.setChecked(savedInstanceState.getBoolean("rememberMe", false));
    }

    private void attemptLogin() {
        String email = netIdEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Config.SUPABASE_URL + "/auth/v1/token?grant_type=password";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(jsonBody, JSON);

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
                        Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();

                try {
                    JSONObject json = new JSONObject(res);

                    if (!response.isSuccessful()) {
                        String msg = json.optString("msg", "Login failed");
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this,
                                        "Login failed: " + msg,
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    if (!json.has("access_token")) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email before logging in.",
                                        Toast.LENGTH_LONG).show()
                        );
                        return;
                    }

                    // ✅ Tokens from Supabase
                    String token = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", null);

                    // Supabase auth response has a "user" object with "id"
                    String userId = null;
                    if (json.has("user")) {
                        JSONObject user = json.getJSONObject("user");
                        userId = user.optString("id", null);
                    }

                    boolean remember = rememberMeCheckBox.isChecked();

                    var editor = getSharedPreferences("APP_PREFS", MODE_PRIVATE).edit();
                    editor.putString("JWT", token);
                    if (refreshToken != null) editor.putString("REFRESH_TOKEN", refreshToken);
                    if (userId != null) editor.putString("USER_ID", userId);

                    // 🔹 store the active user's email for ProfileActivity
                    editor.putString("EMAIL", email);

                    editor.putBoolean("REMEMBER_ME", remember);
                    if (remember) {
                        editor.putString("SAVED_EMAIL", email);
                    } else {
                        editor.remove("SAVED_EMAIL");
                    }
                    editor.apply();

                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, ClubsActivity.class));
                        finish();
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Unexpected response", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }


}