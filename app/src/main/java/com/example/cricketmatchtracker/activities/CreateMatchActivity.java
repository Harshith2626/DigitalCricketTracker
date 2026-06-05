package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateMatchActivity extends AppCompatActivity {

    private static final String TAG = "CreateMatchActivity";
    EditText etTeamA, etTeamB, etOvers;
    Button btnCreateMatch;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_match);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create New Match");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etTeamA = findViewById(R.id.etTeamA);
        etTeamB = findViewById(R.id.etTeamB);
        etOvers = findViewById(R.id.etOvers);
        btnCreateMatch = findViewById(R.id.btnCreateMatch);

        dbRef = FirebaseDatabase.getInstance().getReference("matches");

        btnCreateMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createMatch();
            }
        });
    }

    private void createMatch() {
        String teamA = etTeamA.getText().toString().trim();
        String teamB = etTeamB.getText().toString().trim();
        String oversStr = etOvers.getText().toString().trim();

        if (TextUtils.isEmpty(teamA) || TextUtils.isEmpty(teamB) || TextUtils.isEmpty(oversStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int overs;
        try {
            overs = Integer.parseInt(oversStr);
        } catch (Exception e) {
            overs = 20;
        }

        DatabaseReference newMatchRef = dbRef.push();
        String pushKey = newMatchRef.getKey();
        final String matchId = pushKey != null ? pushKey : "local_" + System.currentTimeMillis();

        Match match = new Match(teamA, teamB, overs);
        match.setMatchId(matchId);

        // Save locally to phone storage
        LocalDataManager.saveMatch(this, match);
        
        // Also push to Firebase in background
        newMatchRef.setValue(match);
        
        Log.d(TAG, "Saved locally and navigating. matchId: " + matchId);
        Intent intent = new Intent(CreateMatchActivity.this, PreMatchSetupActivity.class);
        intent.putExtra("matchId", matchId);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
