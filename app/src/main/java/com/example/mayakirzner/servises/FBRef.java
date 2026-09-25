package com.example.mayakirzner.servises;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FBRef {

    public static final FirebaseAuth refAuth = FirebaseAuth.getInstance();
    public static final FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static final DatabaseReference refUsers = FBDB.getReference("Users");

    public static String uid;
    public static DatabaseReference refUser;

    private FBRef() { }

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