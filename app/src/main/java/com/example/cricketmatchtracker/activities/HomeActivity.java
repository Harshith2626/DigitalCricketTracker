package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.utils.FirebaseUtil;

public class HomeActivity extends AppCompatActivity {

    Button btnOrganizer, btnViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable Firebase persistence for offline support
        try {
            FirebaseUtil.getDatabase().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Already enabled or other initialization issue
        }

        setContentView(R.layout.activity_home);

        btnOrganizer = findViewById(R.id.btnOrganizer);
        btnViewer = findViewById(R.id.btnViewer);

        btnOrganizer.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OrganizerDashboardActivity.class);
            startActivity(intent);
        });

        btnViewer.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ViewerDashboardActivity.class);
            startActivity(intent);
        });
    }
}
