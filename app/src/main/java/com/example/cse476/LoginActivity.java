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

        // connects Java code to the XML layout file
        setContentView(R.layout.activity_login);

        // Initialize views by connecting Java variables to XML elements
        netIdEditText = findViewById(R.id.netIdEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);

        // Set up login button click listener
        // When button is clicked, call attemptLogin() method
        loginButton.setOnClickListener(v -> attemptLogin());
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
        String email = ((EditText)findViewById(R.id.netIdEditText)).getText().toString().trim();
        String password = ((EditText)findViewById(R.id.passwordEditText)).getText().toString().trim();

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
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(res);
                        String token = json.getString("access_token");

                        getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                                .edit()
                                .putString("JWT", token)
                                .apply();

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, ClubsActivity.class));
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Parse error", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

}