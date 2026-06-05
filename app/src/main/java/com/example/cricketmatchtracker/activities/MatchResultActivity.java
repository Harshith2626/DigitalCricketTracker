package com.example.cricketmatchtracker.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
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
        matchRef = FirebaseDatabase.getInstance().getReference("matches").child(matchId);

        tvMatchTitle = findViewById(R.id.tvMatchTitle);
        tvResult = findViewById(R.id.tvResult);
        tvScoreA = findViewById(R.id.tvScoreA);
        tvScoreB = findViewById(R.id.tvScoreB);
        tvOversA = findViewById(R.id.tvOversA);
        tvOversB = findViewById(R.id.tvOversB);
        tvWinner = findViewById(R.id.tvWinner);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        btnBackToHome.setOnClickListener(v -> finish());

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

                    tvMatchTitle.setText(teamA + " vs " + teamB);
                    tvScoreA.setText(teamA + ": " + (scoreA != null ? scoreA : 0) + "/" + (wicketsA != null ? wicketsA : 0));
                    tvScoreB.setText(teamB + ": " + (scoreB != null ? scoreB : 0) + "/" + (wicketsB != null ? wicketsB : 0));
                    
                    tvOversA.setText(String.format(Locale.getDefault(), "(%.1f)", oversA != null ? oversA : 0.0));
                    tvOversB.setText(String.format(Locale.getDefault(), "(%.1f)", oversB != null ? oversB : 0.0));

                    if (winner != null) {
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
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying result: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
