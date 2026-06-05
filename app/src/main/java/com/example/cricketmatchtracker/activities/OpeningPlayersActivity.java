package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class OpeningPlayersActivity extends AppCompatActivity {

    private static final String TAG = "OpeningPlayersActivity";
    TextView tvBattingTeamHeader, tvBowlingTeamHeader;
    Spinner spinnerStriker, spinnerNonStriker, spinnerBowler;
    Button btnStartMatch;
    String matchId, tossWinner, tossDecision;
    DatabaseReference matchRef;
    List<String> battingTeamPlayers = new ArrayList<>();
    List<String> bowlingTeamPlayers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening_players);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Match Start Setup");
        }

        matchId = getIntent().getStringExtra("matchId");
        tossWinner = getIntent().getStringExtra("tossWinner");
        tossDecision = getIntent().getStringExtra("tossDecision");

        tvBattingTeamHeader = findViewById(R.id.tvBattingTeamHeader);
        tvBowlingTeamHeader = findViewById(R.id.tvBowlingTeamHeader);
        spinnerStriker = findViewById(R.id.spinnerStriker);
        spinnerNonStriker = findViewById(R.id.spinnerNonStriker);
        spinnerBowler = findViewById(R.id.spinnerBowler);
        btnStartMatch = findViewById(R.id.btnStartMatch);

        matchRef = FirebaseDatabase.getInstance().getReference("matches").child(matchId);

        matchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String teamA = snapshot.child("teamA").getValue(String.class);
                String teamB = snapshot.child("teamB").getValue(String.class);
                
                String battingTeam = snapshot.child("battingTeam").getValue(String.class);
                boolean teamABats;
                
                if (battingTeam != null) {
                    teamABats = battingTeam.equals("A");
                } else {
                    if (tossWinner == null || tossWinner.isEmpty()) {
                        teamABats = true; 
                    } else if (tossWinner.equals(teamA)) {
                        teamABats = "Bat".equalsIgnoreCase(tossDecision);
                    } else {
                        teamABats = "Bowl".equalsIgnoreCase(tossDecision);
                    }
                }

                List<String> bPlayers;
                List<String> bowlPlayers;

                if (teamABats) {
                    tvBattingTeamHeader.setText(teamA + " (Batting)");
                    tvBowlingTeamHeader.setText("Select Bowler from " + teamB);
                    bPlayers = (List<String>) snapshot.child("teamAPlayers").getValue();
                    bowlPlayers = (List<String>) snapshot.child("teamBPlayers").getValue();
                    matchRef.child("battingTeam").setValue("A");
                } else {
                    tvBattingTeamHeader.setText(teamB + " (Batting)");
                    tvBowlingTeamHeader.setText("Select Bowler from " + teamA);
                    bPlayers = (List<String>) snapshot.child("teamBPlayers").getValue();
                    bowlPlayers = (List<String>) snapshot.child("teamAPlayers").getValue();
                    matchRef.child("battingTeam").setValue("B");
                }

                battingTeamPlayers.clear();
                if (bPlayers != null) battingTeamPlayers.addAll(bPlayers);
                
                bowlingTeamPlayers.clear();
                if (bowlPlayers != null) bowlingTeamPlayers.addAll(bowlPlayers);

                ArrayAdapter<String> batAdapter = new ArrayAdapter<>(
                        OpeningPlayersActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        battingTeamPlayers
                );
                spinnerStriker.setAdapter(batAdapter);
                spinnerNonStriker.setAdapter(batAdapter);
                
                ArrayAdapter<String> bowlAdapter = new ArrayAdapter<>(
                        OpeningPlayersActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        bowlingTeamPlayers
                );
                spinnerBowler.setAdapter(bowlAdapter);
                
                if (battingTeamPlayers.size() > 1) spinnerNonStriker.setSelection(1);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        btnStartMatch.setOnClickListener(v -> {
            if (spinnerStriker.getSelectedItem() == null || spinnerNonStriker.getSelectedItem() == null || spinnerBowler.getSelectedItem() == null) {
                Toast.makeText(this, "Please select all players", Toast.LENGTH_SHORT).show();
                return;
            }

            String striker = spinnerStriker.getSelectedItem().toString();
            String nonStriker = spinnerNonStriker.getSelectedItem().toString();
            String bowler = spinnerBowler.getSelectedItem().toString();

            if (striker.equals(nonStriker)) {
                Toast.makeText(this, "Striker and non-striker must be different!", Toast.LENGTH_SHORT).show();
                return;
            }

            matchRef.child("striker").setValue(striker);
            matchRef.child("nonStriker").setValue(nonStriker);
            matchRef.child("currentBowler").setValue(bowler);
            matchRef.child("started").setValue(true);

            Intent intent = new Intent(this, LiveScoringActivity.class);
            intent.putExtra("matchId", matchId);
            startActivity(intent);
            finish();
        });
    }
}
