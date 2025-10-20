package com.example.cse476;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// FIRST ACTIVITY - handles user login
public class LoginActivity extends AppCompatActivity {

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
    private void attemptLogin() {
        // Get text from input fields
        String netId = netIdEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // create an if statement to check if fields are empty
        if (netId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.enter_cred, Toast.LENGTH_SHORT).show();
            return;
        }


        // Create an Intent to navigate to ClubsActivity
        Intent intent = new Intent(LoginActivity.this, ClubsActivity.class);
        startActivity(intent); // This actually starts the new activity
    }

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
}