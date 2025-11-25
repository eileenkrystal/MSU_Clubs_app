package com.example.cse476;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.*;

import java.io.IOException;

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
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SignUpActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                        finish();       // <-- fixed here
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(SignUpActivity.this, "Signup failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}
