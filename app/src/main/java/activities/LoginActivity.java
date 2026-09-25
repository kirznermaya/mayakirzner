package activities;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import com.example.mayakirzner.databinding.ActivityLoginBinding;

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

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final String PREFS_NAME = "PREFS_NAME";
    private static final String KEY_STAY_CONNECT = "stayConnect";
    private SharedPreferences settings;
    private ActivityLoginBinding binding;
    private boolean loginInProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // БЫЛО:
        // setContentView(R.layout.activity_login);
        // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), ...);

        // СТАЛО:
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        FBRef.initializeGoogleSignIn(this);
        setLoginInProgress(false);

        // SharedPreferences demo: restore and save the "remember me" checkbox.
        binding.cBstayconnect.setChecked(settings.getBoolean(KEY_STAY_CONNECT, false));
        binding.cBstayconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                        settings.edit().putBoolean(KEY_STAY_CONNECT, isChecked).apply()
        );

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

        String email = binding.eTemail.getText().toString().trim();
        String password = binding.eTpass.getText().toString();

        if (TextUtils.isEmpty(email)) {
            binding.eTemail.setError("Enter e-mail");
            binding.eTemail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.eTpass.setError("Enter password");
            binding.eTpass.requestFocus();
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
                        settings.edit().putBoolean(KEY_STAY_CONNECT, binding.cBstayconnect.isChecked()).apply();
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
        binding.btn.setEnabled(!inProgress);
        binding.btn.setText(inProgress ? "Logging in..." : "Login");
        binding.btnGoogleSignIn.setEnabled(!inProgress);
    }

    private void openMenu() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }

    public void onGoogleLoginClick(View view) {
        if (loginInProgress) return;

        if (FBRef.googleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In is not configured yet (check google-services.json)", Toast.LENGTH_LONG).show();
            return;
        }

        Intent signInIntent = FBRef.googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_GOOGLE_SIGN_IN) return;

        if (data == null) {
            Toast.makeText(this, "Google sign-in canceled", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        handleGoogleSignInResult(task);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                Toast.makeText(this, "Google sign-in failed: missing ID token", Toast.LENGTH_LONG).show();
                return;
            }

            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign-in failed (code " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoginInProgress(true);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FBRef.refAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoginInProgress(false);

                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = FBRef.refAuth.getCurrentUser();
                        if (currentUser == null) {
                            Toast.makeText(this, "Google login succeeded but no user was returned", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FBRef.getUser(currentUser);
                        settings.edit().putBoolean(KEY_STAY_CONNECT, binding.cBstayconnect.isChecked()).apply();
                        Toast.makeText(this, "Google login success", Toast.LENGTH_SHORT).show();
                        openMenu();
                        return;
                    }

                    String errorMessage = task.getException() != null
                            ? task.getException().getLocalizedMessage()
                            : "Google Firebase auth failed";

                    Toast.makeText(this, "Google login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
    }
}