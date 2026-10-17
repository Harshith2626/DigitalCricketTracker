package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class PreMatchSetupActivity extends AppCompatActivity {

    // Toss
    RadioGroup rgTossWinner, rgTossDecision;
    RadioButton rbTeamA, rbTeamB, rbBat, rbBowl;

    // Player inputs for Team A
    LinearLayout llTeamAPlayers, llTeamBPlayers;

    Button btnSaveAndProceed;

    String matchId, teamAName, teamBName;
    DatabaseReference matchRef;

    List<EditText> teamAInputs = new ArrayList<>();
    List<EditText> teamBInputs = new ArrayList<>();
    private boolean inputsBuilt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_match_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pre-Match Setup");
        }

        matchId = getIntent().getStringExtra("matchId");
        matchRef = FirebaseUtil.getDatabase().getReference("matches").child(matchId);

        rgTossWinner = findViewById(R.id.rgTossWinner);
        rgTossDecision = findViewById(R.id.rgTossDecision);
        rbTeamA = findViewById(R.id.rbTeamA);
        rbTeamB = findViewById(R.id.rbTeamB);
        rbBat = findViewById(R.id.rbBat);
        rbBowl = findViewById(R.id.rbBowl);
        llTeamAPlayers = findViewById(R.id.llTeamAPlayers);
        llTeamBPlayers = findViewById(R.id.llTeamBPlayers);
        btnSaveAndProceed = findViewById(R.id.btnSaveAndProceed);

        // 1. Load immediately from local storage for offline / instant responsiveness
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            teamAName = localMatch.getTeamA();
            teamBName = localMatch.getTeamB();
            if (teamAName != null) rbTeamA.setText(teamAName);
            if (teamBName != null) rbTeamB.setText(teamBName);
            if (!inputsBuilt && teamAName != null && teamBName != null) {
                buildPlayerInputs(llTeamAPlayers, teamAInputs, teamAName);
                buildPlayerInputs(llTeamBPlayers, teamBInputs, teamBName);
                inputsBuilt = true;
            }
        }

        // 2. Load from Firebase if available
        matchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String fbTeamA = snapshot.child("teamA").getValue(String.class);
                String fbTeamB = snapshot.child("teamB").getValue(String.class);

                if (fbTeamA != null) teamAName = fbTeamA;
                if (fbTeamB != null) teamBName = fbTeamB;

                if (teamAName != null) rbTeamA.setText(teamAName);
                if (teamBName != null) rbTeamB.setText(teamBName);

                if (!inputsBuilt && teamAName != null && teamBName != null) {
                    buildPlayerInputs(llTeamAPlayers, teamAInputs, teamAName);
                    buildPlayerInputs(llTeamBPlayers, teamBInputs, teamBName);
                    inputsBuilt = true;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        btnSaveAndProceed.setOnClickListener(v -> saveSetup());
    }

    private void buildPlayerInputs(LinearLayout layout, List<EditText> inputList, String teamName) {
        TextView header = new TextView(this);
        header.setText(teamName + " Players:");
        header.setTextSize(16);
        header.setPadding(0, 16, 0, 8);
        layout.addView(header);

        for (int i = 1; i <= 11; i++) {
            EditText et = new EditText(this);
            et.setHint("Player " + i);
            et.setPadding(8, 8, 8, 8);
            layout.addView(et);
            inputList.add(et);
        }
    }

    private void saveSetup() {
        // Validate toss
        if (rgTossWinner.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Select toss winner", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgTossDecision.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Select toss decision", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine batting team based on toss winner and decision
        String battingTeam;
        boolean teamAWonToss = (rgTossWinner.getCheckedRadioButtonId() == R.id.rbTeamA);
        boolean choseBat = (rgTossDecision.getCheckedRadioButtonId() == R.id.rbBat);

        if (teamAWonToss) {
            battingTeam = choseBat ? "A" : "B";
        } else {
            battingTeam = choseBat ? "B" : "A";
        }

        String tossWinner = teamAWonToss ? teamAName : teamBName;
        String tossDecision = choseBat ? "Bat" : "Bowl";

        // Collect players
        List<String> teamAPlayers = new ArrayList<>();
        List<String> teamBPlayers = new ArrayList<>();

        int aCount = teamAInputs.size();
        for (int i = 0; i < 11; i++) {
            String name = (i < aCount) ? teamAInputs.get(i).getText().toString().trim() : "";
            if (name.isEmpty()) name = "Player A" + (i + 1);
            teamAPlayers.add(name);
        }
        int bCount = teamBInputs.size();
        for (int i = 0; i < 11; i++) {
            String name = (i < bCount) ? teamBInputs.get(i).getText().toString().trim() : "";
            if (name.isEmpty()) name = "Player B" + (i + 1);
            teamBPlayers.add(name);
        }

        // Save locally for offline support
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            localMatch.setTossWinner(tossWinner);
            localMatch.setTossDecision(tossDecision);
            localMatch.setBattingTeam(battingTeam);
            localMatch.setTeamAPlayers(teamAPlayers);
            localMatch.setTeamBPlayers(teamBPlayers);
            LocalDataManager.saveMatch(this, localMatch);
        }

        // Save to Firebase
        matchRef.child("tossWinner").setValue(tossWinner);
        matchRef.child("tossDecision").setValue(tossDecision);
        matchRef.child("battingTeam").setValue(battingTeam);
        matchRef.child("teamAPlayers").setValue(teamAPlayers);
        matchRef.child("teamBPlayers").setValue(teamBPlayers);
        matchRef.child("started").setValue(false);

        Toast.makeText(this, "Setup saved!", Toast.LENGTH_SHORT).show();

        // Navigate to opening players
        Intent intent = new Intent(this, OpeningPlayersActivity.class);
        intent.putExtra("matchId", matchId);
        intent.putExtra("tossWinner", tossWinner);
        intent.putExtra("tossDecision", tossDecision);
        intent.putExtra("battingTeam", battingTeam); // Pass the corrected batting team info
        startActivity(intent);
        finish();
    }
}
