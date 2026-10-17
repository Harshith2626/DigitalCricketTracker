package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
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
    private boolean playersLoaded = false;

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

        matchRef = FirebaseUtil.getDatabase().getReference("matches").child(matchId);

        // 1. Load instantly from local storage for offline responsiveness
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            populatePlayerData(localMatch.getTeamA(), localMatch.getTeamB(),
                    localMatch.getBattingTeam(), localMatch.getTeamAPlayers(), localMatch.getTeamBPlayers());
        }

        // 2. Sync from Firebase if available
        matchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String teamA = snapshot.child("teamA").getValue(String.class);
                String teamB = snapshot.child("teamB").getValue(String.class);
                String battingTeam = snapshot.child("battingTeam").getValue(String.class);

                List<String> aPlayers = (List<String>) snapshot.child("teamAPlayers").getValue();
                List<String> bPlayers = (List<String>) snapshot.child("teamBPlayers").getValue();

                if (teamA != null && teamB != null) {
                    populatePlayerData(teamA, teamB, battingTeam, aPlayers, bPlayers);
                }
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

            // Save locally
            Match m = LocalDataManager.getMatch(this, matchId);
            if (m != null) {
                m.setStriker(striker);
                m.setNonStriker(nonStriker);
                m.setCurrentBowler(bowler);
                m.setStarted(true);
                LocalDataManager.saveMatch(this, m);
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

    private void populatePlayerData(String teamA, String teamB, String battingTeam, List<String> aPlayers, List<String> bPlayers) {
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

        List<String> bTeamPlayers;
        List<String> bowlTeamPlayers;

        if (teamABats) {
            tvBattingTeamHeader.setText(teamA + " (Batting)");
            tvBowlingTeamHeader.setText("Select Bowler from " + teamB);
            bTeamPlayers = aPlayers;
            bowlTeamPlayers = bPlayers;
            matchRef.child("battingTeam").setValue("A");
        } else {
            tvBattingTeamHeader.setText(teamB + " (Batting)");
            tvBowlingTeamHeader.setText("Select Bowler from " + teamA);
            bTeamPlayers = bPlayers;
            bowlTeamPlayers = aPlayers;
            matchRef.child("battingTeam").setValue("B");
        }

        battingTeamPlayers.clear();
        if (bTeamPlayers != null) battingTeamPlayers.addAll(bTeamPlayers);

        bowlingTeamPlayers.clear();
        if (bowlTeamPlayers != null) bowlingTeamPlayers.addAll(bowlTeamPlayers);

        if (!battingTeamPlayers.isEmpty()) {
            ArrayAdapter<String> batAdapter = new ArrayAdapter<>(
                    OpeningPlayersActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    battingTeamPlayers
            );
            spinnerStriker.setAdapter(batAdapter);
            spinnerNonStriker.setAdapter(batAdapter);
            if (battingTeamPlayers.size() > 1) spinnerNonStriker.setSelection(1);
        }

        if (!bowlingTeamPlayers.isEmpty()) {
            ArrayAdapter<String> bowlAdapter = new ArrayAdapter<>(
                    OpeningPlayersActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    bowlingTeamPlayers
            );
            spinnerBowler.setAdapter(bowlAdapter);
        }
    }
}
