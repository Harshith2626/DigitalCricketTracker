package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_match_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pre-Match Setup");
        }

        matchId = getIntent().getStringExtra("matchId");
        matchRef = FirebaseDatabase.getInstance().getReference("matches").child(matchId);

        rgTossWinner = findViewById(R.id.rgTossWinner);
        rgTossDecision = findViewById(R.id.rgTossDecision);
        rbTeamA = findViewById(R.id.rbTeamA);
        rbTeamB = findViewById(R.id.rbTeamB);
        rbBat = findViewById(R.id.rbBat);
        rbBowl = findViewById(R.id.rbBowl);
        llTeamAPlayers = findViewById(R.id.llTeamAPlayers);
        llTeamBPlayers = findViewById(R.id.llTeamBPlayers);
        btnSaveAndProceed = findViewById(R.id.btnSaveAndProceed);

        // Load team names from Firebase to set button labels
        matchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                teamAName = snapshot.child("teamA").getValue(String.class);
                teamBName = snapshot.child("teamB").getValue(String.class);

                rbTeamA.setText(teamAName);
                rbTeamB.setText(teamBName);

                // Build player input fields dynamically
                buildPlayerInputs(llTeamAPlayers, teamAInputs, teamAName);
                buildPlayerInputs(llTeamBPlayers, teamBInputs, teamBName);
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

        for (int i = 0; i < 11; i++) {
            String name = teamAInputs.get(i).getText().toString().trim();
            if (name.isEmpty()) name = "Player A" + (i + 1);
            teamAPlayers.add(name);
        }
        for (int i = 0; i < 11; i++) {
            String name = teamBInputs.get(i).getText().toString().trim();
            if (name.isEmpty()) name = "Player B" + (i + 1);
            teamBPlayers.add(name);
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
