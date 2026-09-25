package activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import com.example.mayakirzner.servises.FBRef;
import com.google.firebase.auth.FirebaseUser;

import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mayakirzner.R;
import activities.MenuActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "PREFS_NAME";
    private static final String KEY_STAY_CONNECT = "stayConnect";
    private SharedPreferences settings;
    private CheckBox cBstayconnect;
    private EditText eTemail;
    private EditText eTpass;
    private Button btnLogin;
    private boolean loginInProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        initViews();
        setLoginInProgress(false);

        // SharedPreferences demo: restore and save the "remember me" checkbox.
        cBstayconnect.setChecked(settings.getBoolean(KEY_STAY_CONNECT, false));
        cBstayconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                        settings.edit().putBoolean(KEY_STAY_CONNECT, isChecked).apply()
        );

    }
    private void initViews() {
        cBstayconnect = findViewById(R.id.cBstayconnect);
        eTemail = findViewById(R.id.eTemail);
        eTpass = findViewById(R.id.eTpass);
        btnLogin = findViewById(R.id.btn);
    }
    @Override
    protected void onStart() {
        super.onStart();
        boolean stayConnected = settings.getBoolean(KEY_STAY_CONNECT, false);
        FirebaseUser currentUser = FBRef.refAuth.getCurrentUser();
        if (currentUser != null && stayConnected) {
            FBRef.getUser(currentUser);
            openMenu();
        }
    }
    public void onLoginClick(View view) {
        if (loginInProgress) return;

        String email = eTemail.getText().toString().trim();
        String password = eTpass.getText().toString();

        if (TextUtils.isEmpty(email)) {
            eTemail.setError("Enter e-mail");
            eTemail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            eTpass.setError("Enter password");
            eTpass.requestFocus();
            return;
        }

        setLoginInProgress(true);

        FBRef.refAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoginInProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = FBRef.refAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "Login succeeded but no user was returned", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FBRef.getUser(user);
                        settings.edit().putBoolean(KEY_STAY_CONNECT, cBstayconnect.isChecked()).apply();
                        Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();
                        openMenu();
                        return;
                    }
                    String errorMessage = (task.getException() != null)
                            ? task.getException().getLocalizedMessage()
                            : "Login failed";
                    Toast.makeText(this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
    }
    private void setLoginInProgress(boolean inProgress) {
        loginInProgress = inProgress;
        btnLogin.setEnabled(!inProgress);
        btnLogin.setText(inProgress ? "Logging in..." : "Login");
    }

    private void openMenu() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }

    public void onGoogleLoginClick(View view) {
        Toast.makeText(this, "Google sign-in is an advanced later phase", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}