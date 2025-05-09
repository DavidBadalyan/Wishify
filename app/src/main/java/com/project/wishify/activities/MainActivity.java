package com.project.wishify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.wishify.R;
import com.project.wishify.fragments.CalendarFragment;
import com.project.wishify.fragments.ContactsFragment;
import com.project.wishify.fragments.GiftFragment;
import com.project.wishify.fragments.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AppCompatButton settingsButton;
    private FirebaseAuth auth;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();

            setContentView(R.layout.activity_main);
            Log.d(TAG, "Layout set: activity_main");

            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            settingsButton = findViewById(R.id.settingsButton);
            Log.d(TAG, "Views initialized: drawerLayout, navigationView, settingsButton");

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "No user signed in, redirecting to LoginActivity");
                goToLoginActivity();
                return;
            }

            Log.d(TAG, "User signed in: " + currentUser.getEmail());
            TextView emailTextView = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
            emailTextView.setText(currentUser.getEmail());
            TextView nameTextView = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
            nameTextView.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");

            settingsButton.setOnClickListener(v -> {
                Log.d(TAG, "Settings button clicked, opening drawer");
                drawerLayout.openDrawer(GravityCompat.END);
            });

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    Log.d(TAG, "Starting ProfileActivity");
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_settings) {
                    Log.d(TAG, "Starting SettingsActivity");
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_about) {
                    Log.d(TAG, "Starting AboutActivity");
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_logout) {
                    try {
                        Log.d(TAG, "Logging out user");
                        auth.signOut();
                        goToLoginActivity();
                    } catch (Exception e) {
                        Toast.makeText(this, "Logout failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            });

            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Log.d(TAG, "Navigating to HomeFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                    return true;
                } else if (id == R.id.nav_calendar) {
                    Log.d(TAG, "Navigating to CalendarFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new CalendarFragment())
                            .commit();
                    return true;
                } else if (id == R.id.nav_contacts) {
                    Log.d(TAG, "Navigating to ContactsFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new ContactsFragment())
                            .commit();
                    return true;
                } else if (id == R.id.nav_gift) {
                    Log.d(TAG, "Navigating to GiftsFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new GiftFragment())
                            .commit();
                    return true;
                }
                return false;
            });

            if (savedInstanceState == null) {
                Log.d(TAG, "Initial navigation to HomeFragment");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        } catch (Exception e) {
            Log.e(TAG, "Crash in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            goToLoginActivity();
        }
    }

    private void goToLoginActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LoginActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to start LoginActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}