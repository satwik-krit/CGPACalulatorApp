package com.example.cgpacalculator;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment), (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });
//        View root = findViewById(R.id.root_layout);
//
//        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//
//            v.setPadding(
//                    v.getPaddingLeft(),
//                    v.getPaddingTop(),
//                    v.getPaddingRight(),
//                    systemBars.bottom
//            );
//
//            return insets;
//        });
        // wire bottom nav to the nav controller
        NavHostFragment navHostFragment =
                (NavHostFragment)getSupportFragmentManager() .findFragmentById(R.id.nav_host_fragment);

        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav =
                findViewById(R.id.bottomNavView);

        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}