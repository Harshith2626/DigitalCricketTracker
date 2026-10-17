package com.example.cricketmatchtracker.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;

import java.util.Locale;

public class MatchResultActivity extends AppCompatActivity {

    private static final String TAG = "MatchResultActivity";
    TextView tvMatchTitle, tvResult, tvScoreA, tvScoreB, tvWinner, tvOversA, tvOversB;
    Button btnBackToHome;
    String matchId;
    DatabaseReference matchRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_result);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Match Result");
        }

        matchId = getIntent().getStringExtra("matchId");
        if (matchId == null) {
            finish();
            return;
        }

        tvMatchTitle = findViewById(R.id.tvMatchTitle);
        tvResult = findViewById(R.id.tvResult);
        tvScoreA = findViewById(R.id.tvScoreA);
        tvScoreB = findViewById(R.id.tvScoreB);
        tvOversA = findViewById(R.id.tvOversA);
        tvOversB = findViewById(R.id.tvOversB);
        tvWinner = findViewById(R.id.tvWinner);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        btnBackToHome.setOnClickListener(v -> finish());

        // 1. Display instantly from local storage
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            displayMatchResult(localMatch.getTeamA(), localMatch.getTeamB(),
                    localMatch.getScoreA(), localMatch.getScoreB(),
                    localMatch.getWicketsA(), localMatch.getWicketsB(),
                    localMatch.getOversA(), localMatch.getOversB(),
                    localMatch.getWinner());
        }

        // 2. Fetch latest from Firebase
        matchRef = FirebaseUtil.getDatabase().getReference("matches").child(matchId);

        matchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                
                try {
                    String teamA = snapshot.child("teamA").getValue(String.class);
                    String teamB = snapshot.child("teamB").getValue(String.class);
                    
                    Integer scoreA = snapshot.child("scoreA").getValue(Integer.class);
                    Integer scoreB = snapshot.child("scoreB").getValue(Integer.class);
                    Integer wicketsA = snapshot.child("wicketsA").getValue(Integer.class);
                    Integer wicketsB = snapshot.child("wicketsB").getValue(Integer.class);
                    
                    Double oversA = snapshot.child("oversA").getValue(Double.class);
                    Double oversB = snapshot.child("oversB").getValue(Double.class);
                    
                    String winner = snapshot.child("winner").getValue(String.class);

                    displayMatchResult(teamA, teamB,
                            scoreA != null ? scoreA : 0, scoreB != null ? scoreB : 0,
                            wicketsA != null ? wicketsA : 0, wicketsB != null ? wicketsB : 0,
                            oversA != null ? oversA : 0.0, oversB != null ? oversB : 0.0,
                            winner);
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying result: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayMatchResult(String teamA, String teamB, int scoreA, int scoreB,
                                    int wicketsA, int wicketsB, double oversA, double oversB, String winner) {
        if (teamA == null || teamB == null) return;
        tvMatchTitle.setText(teamA + " vs " + teamB);
        tvScoreA.setText(teamA + ": " + scoreA + "/" + wicketsA);
        tvScoreB.setText(teamB + ": " + scoreB + "/" + wicketsB);

        tvOversA.setText(String.format(Locale.getDefault(), "(%.1f)", oversA));
        tvOversB.setText(String.format(Locale.getDefault(), "(%.1f)", oversB));

        if (winner != null && !winner.isEmpty()) {
            if (winner.equals("Tie")) {
                tvResult.setText("Match Result: Tie!");
                tvWinner.setText("It's a Tie!");
            } else {
                tvResult.setText("Match Completed");
                tvWinner.setText(winner + " won");
            }
        } else {
            tvWinner.setText("Result Pending");
        }
    }
}
