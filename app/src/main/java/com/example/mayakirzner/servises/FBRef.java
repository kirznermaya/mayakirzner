package com.example.mayakirzner.servises;

import android.content.Context;
import android.util.Log;

import com.example.mayakirzner.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FBRef {

    public static final FirebaseAuth refAuth = FirebaseAuth.getInstance();
    public static final FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static final DatabaseReference refUsers = FBDB.getReference("Users");

    public static GoogleSignInClient googleSignInClient;
    public static String uid;
    public static DatabaseReference refUser;

    private FBRef() { }

    public static void initializeGoogleSignIn(Context context) {
        try {
            String webClientId = context.getString(R.string.default_web_client_id);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(context, gso);
        } catch (Exception e) {
            Log.e("FBRef", "Failed to initialize Google Sign-In client", e);
            googleSignInClient = null;
        }
    }

    public static void signOutGoogle() {
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }

    public static void getUser(FirebaseUser fbuser) {
        if (fbuser == null) {
            uid = null;
            refUser = null;
            return;
        }

        uid = fbuser.getUid();
        refUser = refUsers.child(uid);
    }
}