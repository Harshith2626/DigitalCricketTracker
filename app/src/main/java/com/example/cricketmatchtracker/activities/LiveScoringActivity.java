package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LiveScoringActivity extends AppCompatActivity {

    private static final String TAG = "LiveScoringActivity";

    TextView tvMatchTitle, tvScore, tvOvers, tvStriker, tvNonStriker, tvInnings, tvTarget, tvBowler;
    Button btn0, btn1, btn2, btn3, btn4, btn6, btnWide, btnNoBall, btnWicket, btnUndo;

    String matchId;
    DatabaseReference matchRef;
    Match currentMatch;

    int currentScore = 0, currentWickets = 0, totalBalls = 0, totalOvers = 0, currentInnings = 1, targetScore = 0;
    String battingTeam = "A", striker = "", nonStriker = "", currentBowler = "", teamAName = "", teamBName = "";

    Map<String, Integer> playerRuns = new HashMap<>(), playerBalls = new HashMap<>();
    Map<String, Integer> bowlerRuns = new HashMap<>(), bowlerWickets = new HashMap<>(), bowlerBalls = new HashMap<>();
    List<String> battingPlayers = new ArrayList<>(), bowlingPlayers = new ArrayList<>(), outPlayers = new ArrayList<>();

    int lastScore, lastWickets, lastTotalBalls;
    String lastStriker, lastNonStriker, lastBowler;
    int lastStrikerRuns, lastStrikerBalls, lastNonStrikerRuns, lastNonStrikerBalls, lastBowlerRuns, lastBowlerWickets, lastBowlerBalls;
    boolean lastActionValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_scoring);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Scoring");

        matchId = getIntent().getStringExtra("matchId");
        if (matchId == null) {
            finish();
            return;
        }
        matchRef = FirebaseUtil.getDatabase().getReference("matches").child(matchId);

        initViews();
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            applyMatchData(localMatch);
        }
        loadMatchData();
    }

    private void initViews() {
        tvMatchTitle = findViewById(R.id.tvMatchTitle);
        tvScore = findViewById(R.id.tvScore);
        tvOvers = findViewById(R.id.tvOvers);
        tvStriker = findViewById(R.id.tvStriker);
        tvNonStriker = findViewById(R.id.tvNonStriker);
        tvInnings = findViewById(R.id.tvInnings);
        tvTarget = findViewById(R.id.tvTarget);
        tvBowler = findViewById(R.id.tvBowler);

        btn0 = findViewById(R.id.btn0); btn1 = findViewById(R.id.btn1); btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3); btn4 = findViewById(R.id.btn4); btn6 = findViewById(R.id.btn6);
        btnWide = findViewById(R.id.btnWide); btnNoBall = findViewById(R.id.btnNoBall);
        btnWicket = findViewById(R.id.btnWicket); btnUndo = findViewById(R.id.btnUndo);

        btn0.setOnClickListener(v -> addRuns(0, true));
        btn1.setOnClickListener(v -> addRuns(1, true));
        btn2.setOnClickListener(v -> addRuns(2, true));
        btn3.setOnClickListener(v -> addRuns(3, true));
        btn4.setOnClickListener(v -> addRuns(4, true));
        btn6.setOnClickListener(v -> addRuns(6, true));
        btnWide.setOnClickListener(v -> addExtra("Wide"));
        btnNoBall.setOnClickListener(v -> addExtra("No Ball"));
        btnWicket.setOnClickListener(v -> handleWicket());
        btnUndo.setOnClickListener(v -> undoLastBall());
    }

    private void applyMatchData(Match match) {
        if (match == null) return;
        currentMatch = match;
        teamAName = currentMatch.getTeamA();
        teamBName = currentMatch.getTeamB();
        totalOvers = currentMatch.getTotalOvers();
        currentInnings = currentMatch.getCurrentInnings();
        battingTeam = currentMatch.getBattingTeam();
        striker = currentMatch.getStriker();
        nonStriker = currentMatch.getNonStriker();
        currentBowler = currentMatch.getCurrentBowler();

        if ("A".equals(battingTeam)) {
            currentScore = currentMatch.getScoreA();
            currentWickets = currentMatch.getWicketsA();
            battingPlayers = currentMatch.getTeamAPlayers() != null ? currentMatch.getTeamAPlayers() : new ArrayList<>();
            bowlingPlayers = currentMatch.getTeamBPlayers() != null ? currentMatch.getTeamBPlayers() : new ArrayList<>();
            double o = currentMatch.getOversA();
            totalBalls = (int)(Math.floor(o)*6 + Math.round((o - Math.floor(o))*10));
        } else {
            currentScore = currentMatch.getScoreB();
            currentWickets = currentMatch.getWicketsB();
            battingPlayers = currentMatch.getTeamBPlayers() != null ? currentMatch.getTeamBPlayers() : new ArrayList<>();
            bowlingPlayers = currentMatch.getTeamAPlayers() != null ? currentMatch.getTeamAPlayers() : new ArrayList<>();
            double o = currentMatch.getOversB();
            totalBalls = (int)(Math.floor(o)*6 + Math.round((o - Math.floor(o))*10));
        }

        if (currentInnings == 2) {
            int s1 = "A".equals(battingTeam) ? currentMatch.getScoreB() : currentMatch.getScoreA();
            targetScore = s1 + 1;
        }
        updateUI();
    }

    private void loadMatchData() {
        matchRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Match match = snapshot.getValue(Match.class);
                if (match == null) return;
                match.setMatchId(snapshot.getKey());

                LocalDataManager.saveMatch(LiveScoringActivity.this, match);

                List<String> fbOut = (List<String>) snapshot.child("outPlayers").getValue();
                if (fbOut != null) {
                    outPlayers.clear();
                    outPlayers.addAll(fbOut);
                }

                loadStats(snapshot.child("stats"));
                applyMatchData(match);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStats(DataSnapshot statsSnapshot) {
        playerRuns.clear(); playerBalls.clear(); bowlerRuns.clear(); bowlerWickets.clear(); bowlerBalls.clear();
        for (DataSnapshot ds : statsSnapshot.child("batsmen").getChildren()) {
            Integer r = ds.child("runs").getValue(Integer.class);
            Integer b = ds.child("balls").getValue(Integer.class);
            playerRuns.put(ds.getKey(), r != null ? r : 0);
            playerBalls.put(ds.getKey(), b != null ? b : 0);
        }
        for (DataSnapshot ds : statsSnapshot.child("bowlers").getChildren()) {
            Integer r = ds.child("runs").getValue(Integer.class);
            Integer w = ds.child("wickets").getValue(Integer.class);
            Integer b = ds.child("balls").getValue(Integer.class);
            bowlerRuns.put(ds.getKey(), r != null ? r : 0);
            bowlerWickets.put(ds.getKey(), w != null ? w : 0);
            bowlerBalls.put(ds.getKey(), b != null ? b : 0);
        }
    }

    private void updateUI() {
        tvMatchTitle.setText(String.format("%s vs %s", teamAName, teamBName));
        tvScore.setText(String.format(Locale.getDefault(), "%d/%d", currentScore, currentWickets));
        tvOvers.setText(String.format(Locale.getDefault(), "Overs: %d.%d / %d", totalBalls / 6, totalBalls % 6, totalOvers));

        double crr = (totalBalls > 0) ? (currentScore * 6.0 / totalBalls) : 0.0;
        String status = String.format(Locale.getDefault(), "Innings %d | CRR: %.2f", currentInnings, crr);

        if (currentInnings == 2) {
            int need = targetScore - currentScore;
            int left = (totalOvers * 6) - totalBalls;
            double rrr = (left > 0) ? (Math.max(0, (double)need) * 6.0 / left) : 0.0;
            status += String.format(Locale.getDefault(), " | RRR: %.2f", rrr);
            tvTarget.setText(String.format(Locale.getDefault(), "Target: %d | Need %d off %d balls", targetScore, need, left));
            tvTarget.setVisibility(View.VISIBLE);
        } else tvTarget.setVisibility(View.GONE);
        tvInnings.setText(status);

        String sName = (striker != null && !striker.isEmpty()) ? striker : "—";
        String nsName = (nonStriker != null && !nonStriker.isEmpty()) ? nonStriker : "—";
        tvStriker.setText(String.format(Locale.getDefault(), "%s: %d(%d)*", sName, playerRuns.getOrDefault(sName, 0), playerBalls.getOrDefault(sName, 0)));
        tvNonStriker.setText(String.format(Locale.getDefault(), "%s: %d(%d)", nsName, playerRuns.getOrDefault(nsName, 0), playerBalls.getOrDefault(nsName, 0)));

        String bName = (currentBowler != null && !currentBowler.isEmpty()) ? currentBowler : "—";
        tvBowler.setText(String.format(Locale.getDefault(), "Bowler: %s %d-%d (%d.%d)", bName, bowlerWickets.getOrDefault(bName, 0), bowlerRuns.getOrDefault(bName, 0), bowlerBalls.getOrDefault(bName, 0) / 6, bowlerBalls.getOrDefault(bName, 0) % 6));
    }

    private void addRuns(int runs, boolean valid) {
        saveLastState();
        currentScore += runs;
        if (striker != null && !striker.isEmpty()) {
            playerRuns.put(striker, playerRuns.getOrDefault(striker, 0) + runs);
            if (valid) playerBalls.put(striker, playerBalls.getOrDefault(striker, 0) + 1);
        }
        if (currentBowler != null && !currentBowler.isEmpty()) {
            bowlerRuns.put(currentBowler, bowlerRuns.getOrDefault(currentBowler, 0) + runs);
            if (valid) {
                totalBalls++;
                bowlerBalls.put(currentBowler, bowlerBalls.getOrDefault(currentBowler, 0) + 1);
                if (runs == 1 || runs == 3) swapStrike();
                if (totalBalls % 6 == 0) {
                    swapStrike(); saveToFirebase();
                    if (!checkInningsEnd()) selectNextBowler();
                    return;
                }
            }
        }
        saveToFirebase();
        checkInningsEnd();
    }

    private void handleWicket() {
        saveLastState();
        currentWickets++; totalBalls++;
        if (currentBowler != null && !currentBowler.isEmpty()) {
            bowlerWickets.put(currentBowler, bowlerWickets.getOrDefault(currentBowler, 0) + 1);
            bowlerBalls.put(currentBowler, bowlerBalls.getOrDefault(currentBowler, 0) + 1);
        }
        if (striker != null && !striker.isEmpty()) {
            playerBalls.put(striker, playerBalls.getOrDefault(striker, 0) + 1);
            if (!outPlayers.contains(striker)) {
                outPlayers.add(striker);
            }
        }
        matchRef.child("outPlayers").setValue(outPlayers);
        saveToFirebase();
        if (!checkInningsEnd()) selectNextBatsman(totalBalls % 6 == 0);
    }

    private void addExtra(String type) {
        saveLastState();
        currentScore += 1;
        if (currentBowler != null && !currentBowler.isEmpty()) {
            bowlerRuns.put(currentBowler, bowlerRuns.getOrDefault(currentBowler, 0) + 1);
        }
        saveToFirebase();
        checkInningsEnd();
    }

    private void saveToFirebase() {
        if (currentMatch == null) return;
        double overs = (totalBalls / 6) + ((totalBalls % 6) * 0.1);
        if ("A".equals(battingTeam)) {
            currentMatch.setScoreA(currentScore); currentMatch.setWicketsA(currentWickets); currentMatch.setOversA(overs);
        } else {
            currentMatch.setScoreB(currentScore); currentMatch.setWicketsB(currentWickets); currentMatch.setOversB(overs);
        }
        currentMatch.setStriker(striker); currentMatch.setNonStriker(nonStriker); currentMatch.setCurrentBowler(currentBowler);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("scoreA", currentMatch.getScoreA());
        updates.put("scoreB", currentMatch.getScoreB());
        updates.put("wicketsA", currentMatch.getWicketsA());
        updates.put("wicketsB", currentMatch.getWicketsB());
        updates.put("oversA", currentMatch.getOversA());
        updates.put("oversB", currentMatch.getOversB());
        updates.put("striker", striker);
        updates.put("nonStriker", nonStriker);
        updates.put("currentBowler", currentBowler);

        if (striker != null && !striker.isEmpty()) {
            updates.put("stats/batsmen/" + striker + "/runs", playerRuns.getOrDefault(striker, 0));
            updates.put("stats/batsmen/" + striker + "/balls", playerBalls.getOrDefault(striker, 0));
        }
        if (nonStriker != null && !nonStriker.isEmpty()) {
            updates.put("stats/batsmen/" + nonStriker + "/runs", playerRuns.getOrDefault(nonStriker, 0));
            updates.put("stats/batsmen/" + nonStriker + "/balls", playerBalls.getOrDefault(nonStriker, 0));
        }
        if (currentBowler != null && !currentBowler.isEmpty()) {
            updates.put("stats/bowlers/" + currentBowler + "/runs", bowlerRuns.getOrDefault(currentBowler, 0));
            updates.put("stats/bowlers/" + currentBowler + "/wickets", bowlerWickets.getOrDefault(currentBowler, 0));
            updates.put("stats/bowlers/" + currentBowler + "/balls", bowlerBalls.getOrDefault(currentBowler, 0));
        }

        matchRef.updateChildren(updates);
        LocalDataManager.saveMatch(this, currentMatch);
    }

    private void swapStrike() {
        String temp = striker; striker = nonStriker; nonStriker = temp;
    }

    private void saveLastState() {
        lastScore = currentScore; lastWickets = currentWickets; lastTotalBalls = totalBalls;
        lastStriker = striker; lastNonStriker = nonStriker; lastBowler = currentBowler;
        lastStrikerRuns = playerRuns.getOrDefault(striker, 0); lastStrikerBalls = playerBalls.getOrDefault(striker, 0);
        lastNonStrikerRuns = playerRuns.getOrDefault(nonStriker, 0); lastNonStrikerBalls = playerBalls.getOrDefault(nonStriker, 0);
        lastBowlerRuns = bowlerRuns.getOrDefault(currentBowler, 0); lastBowlerWickets = bowlerWickets.getOrDefault(currentBowler, 0); lastBowlerBalls = bowlerBalls.getOrDefault(currentBowler, 0);
        lastActionValid = true;
    }

    private boolean checkInningsEnd() {
        if (currentInnings == 2) {
            if (currentScore >= targetScore) { determineWinner(); return true; }
            if (totalBalls >= totalOvers * 6 || currentWickets >= 10) { determineWinner(); return true; }
        } else {
            if (totalBalls >= totalOvers * 6 || currentWickets >= 10) {
                showInningsSwitchDialog();
                return true;
            }
        }
        return false;
    }

    private void showInningsSwitchDialog() {
        new AlertDialog.Builder(this).setTitle("Innings Over").setMessage("Start 2nd Innings?").setPositiveButton("Yes", (d, w) -> switchInnings()).setCancelable(false).show();
    }

    private void switchInnings() {
        battingTeam = battingTeam.equals("A") ? "B" : "A";
        matchRef.child("currentInnings").setValue(2);
        matchRef.child("battingTeam").setValue(battingTeam);
        matchRef.child("striker").setValue("");
        matchRef.child("nonStriker").setValue("");
        matchRef.child("currentBowler").setValue("");
        outPlayers.clear();
        matchRef.child("outPlayers").setValue(outPlayers);

        if (currentMatch != null) {
            currentMatch.setCurrentInnings(2);
            currentMatch.setBattingTeam(battingTeam);
            currentMatch.setStriker("");
            currentMatch.setNonStriker("");
            currentMatch.setCurrentBowler("");
            LocalDataManager.saveMatch(this, currentMatch);
        }

        Intent intent = new Intent(this, OpeningPlayersActivity.class);
        intent.putExtra("matchId", matchId);
        intent.putExtra("battingTeam", battingTeam);
        startActivity(intent);
        finish();
    }

    private void determineWinner() {
        String winner;
        if (currentInnings == 2) {
            int scoreA = currentMatch.getScoreA();
            int scoreB = currentMatch.getScoreB();
            if (battingTeam.equals("B")) scoreB = currentScore; else scoreA = currentScore;

            if (scoreA > scoreB) winner = teamAName;
            else if (scoreB > scoreA) winner = teamBName;
            else winner = "Tie";
        } else {
            // Should not happen with current logic, but for safety
            winner = "Pending";
        }
        
        matchRef.child("completed").setValue(true); 
        matchRef.child("winner").setValue(winner);
        
        // Update local object too before saving
        currentMatch.setCompleted(true);
        currentMatch.setWinner(winner);
        LocalDataManager.saveMatch(this, currentMatch);

        startActivity(new Intent(this, MatchResultActivity.class).putExtra("matchId", matchId));
        finish();
    }

    private void selectNextBowler() {
        List<String> others = new ArrayList<>(bowlingPlayers); if (currentBowler != null) others.remove(currentBowler);
        String[] p = others.toArray(new String[0]);
        if (p.length == 0) return;
        new AlertDialog.Builder(this).setTitle("Next Bowler").setItems(p, (d, w) -> {
            currentBowler = p[w]; 
            matchRef.child("currentBowler").setValue(currentBowler);
            if (currentMatch != null) {
                currentMatch.setCurrentBowler(currentBowler);
                LocalDataManager.saveMatch(this, currentMatch);
            }
            updateUI();
        }).setCancelable(false).show();
    }

    private void selectNextBatsman(boolean overEnded) {
        List<String> available = new ArrayList<>();
        for (String p : battingPlayers) if (!outPlayers.contains(p) && !p.equals(nonStriker)) available.add(p);
        if (available.isEmpty()) return;
        String[] p = available.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Next Batsman").setItems(p, (d, w) -> {
            striker = p[w]; 
            matchRef.child("striker").setValue(striker);
            if (currentMatch != null) {
                currentMatch.setStriker(striker);
                LocalDataManager.saveMatch(this, currentMatch);
            }
            updateUI();
            if (overEnded) { swapStrike(); selectNextBowler(); }
        }).setCancelable(false).show();
    }

    private void undoLastBall() {
        if (!lastActionValid) return;
        currentScore = lastScore; currentWickets = lastWickets; totalBalls = lastTotalBalls;
        striker = lastStriker; nonStriker = lastNonStriker; currentBowler = lastBowler;
        playerRuns.put(striker, lastStrikerRuns); playerBalls.put(striker, lastStrikerBalls);
        playerRuns.put(nonStriker, lastNonStrikerRuns); playerBalls.put(nonStriker, lastNonStrikerBalls);
        bowlerRuns.put(currentBowler, lastBowlerRuns); bowlerWickets.put(lastBowler, lastBowlerWickets); bowlerBalls.put(lastBowler, lastBowlerBalls);
        lastActionValid = false; saveToFirebase();
    }
}
