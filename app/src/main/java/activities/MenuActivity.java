package activities;

import android.os.Bundle;

import android.content.SharedPreferences;
import com.example.mayakirzner.servises.FBRef;
import com.example.mayakirzner.databinding.ActivityMenuBinding;
import activities.LoginActivity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.mayakirzner.HomeFragment;
import com.example.mayakirzner.Main2Activity;
import com.example.mayakirzner.MainActivity;
import com.example.mayakirzner.ProfileFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import com.example.mayakirzner.R;

public class MenuActivity extends AppCompatActivity {
    private ActivityMenuBinding binding;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        NavigationView navigationView = binding.navigationView;

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.nav_local_game) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (item.getItemId() == R.id.nav_rtdb_prep) {
                startActivity(new Intent(this, Main2Activity.class)); //Запуск нового экрана
            } else if (item.getItemId() == R.id.nav_profile) {
                showFragment(new ProfileFragment());
            } else if (item.getItemId() == R.id.nav_logout) {
            logoutAndOpenLogin();
        }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }
    private void logoutAndOpenLogin() {
        SharedPreferences settings = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        settings.edit().putBoolean("stayConnect", false).apply();

        FBRef.signOutGoogle(); // <--- ДОБАВИТЬ ЭТУ СТРОКУ
        FBRef.refAuth.signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, fragment)
                .commit();
    }
}