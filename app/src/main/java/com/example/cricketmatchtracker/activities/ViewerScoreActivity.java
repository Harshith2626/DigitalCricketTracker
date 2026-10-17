package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;
import java.util.Locale;

public class ViewerScoreActivity extends AppCompatActivity {

    private static final String TAG = "ViewerScoreActivity";
    TextView tvMatchTitle, tvInnings, tvScore, tvOvers, tvStriker, tvNonStriker, tvTarget, tvBowler, tvStatusBadge;

    String matchId;
    DatabaseReference matchRef;
    private ValueEventListener scoreListener;
    private boolean isCompletedHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer_score);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Match Scoreboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        matchId = getIntent().getStringExtra("matchId");
        if (matchId == null) {
            finish();
            return;
        }

        initViews();
        
        // 1. Load instantly from local storage (Phone storage)
        Match localMatch = LocalDataManager.getMatch(this, matchId);
        if (localMatch != null) {
            updateUI(localMatch, null); // Pass null for stats if not available locally yet
        }

        // 2. Start background sync with Firebase
        matchRef = FirebaseUtil.getDatabase().getReference("matches").child(matchId);
        startListeningForUpdates();
    }

    private void initViews() {
        tvMatchTitle = findViewById(R.id.tvMatchTitle);
        tvInnings = findViewById(R.id.tvInnings);
        tvScore = findViewById(R.id.tvScore);
        tvOvers = findViewById(R.id.tvOvers);
        tvStriker = findViewById(R.id.tvStriker);
        tvNonStriker = findViewById(R.id.tvNonStriker);
        tvTarget = findViewById(R.id.tvTarget);
        tvBowler = findViewById(R.id.tvBowler);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
    }

    private void startListeningForUpdates() {
        scoreListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Match match = snapshot.getValue(Match.class);
                if (match == null) return;
                match.setMatchId(snapshot.getKey());

                // Save to local storage in background
                LocalDataManager.saveMatch(ViewerScoreActivity.this, match);

                updateUI(match, snapshot.child("stats"));

                if (match.isCompleted() && !isCompletedHandled) {
                    isCompletedHandled = true;
                    handleMatchCompletion();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Sync error: " + error.getMessage());
            }
        };
        matchRef.addValueEventListener(scoreListener);
    }

    private void handleMatchCompletion() {
        if (tvStatusBadge != null) {
            tvStatusBadge.setText("COMPLETED");
            tvStatusBadge.setTextColor(Color.parseColor("#757575"));
        }
        Toast.makeText(this, "Match Finished!", Toast.LENGTH_SHORT).show();
        
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(this, MatchResultActivity.class);
                intent.putExtra("matchId", matchId);
                startActivity(intent);
                finish();
            }
        }, 2500);
    }

    private void updateUI(Match match, DataSnapshot statsSnapshot) {
        tvMatchTitle.setText(String.format("%s vs %s", match.getTeamA(), match.getTeamB()));
        
        // Update Status Badge
        if (tvStatusBadge != null) {
            if (match.isCompleted()) {
                tvStatusBadge.setText("COMPLETED");
                tvStatusBadge.setTextColor(Color.parseColor("#757575"));
            } else if (match.isStarted()) {
                tvStatusBadge.setText("● LIVE");
                tvStatusBadge.setTextColor(Color.parseColor("#FF5252"));
            } else {
                tvStatusBadge.setText("UPCOMING");
                tvStatusBadge.setTextColor(Color.parseColor("#1976D2"));
            }
        }

        String bTeam = match.getBattingTeam();
        int score = "A".equals(bTeam) ? match.getScoreA() : match.getScoreB();
        int wickets = "A".equals(bTeam) ? match.getWicketsA() : match.getWicketsB();
        double oversVal = "A".equals(bTeam) ? match.getOversA() : match.getOversB();
        
        int oversDone = (int) Math.floor(oversVal);
        int ballsDone = (int) Math.round((oversVal - oversDone) * 10);
        int totalBallsDone = (oversDone * 6) + ballsDone;

        double crr = (totalBallsDone > 0) ? (score * 6.0 / totalBallsDone) : 0.0;
        String info = String.format(Locale.getDefault(), "Innings %d | CRR: %.2f", match.getCurrentInnings(), crr);

        if (match.getCurrentInnings() == 2) {
            int firstScore = "A".equals(bTeam) ? match.getScoreB() : match.getScoreA();
            int target = firstScore + 1;
            int need = target - score;
            int ballsLeft = (match.getTotalOvers() * 6) - totalBallsDone;
            double rrr = (ballsLeft > 0) ? (Math.max(0, (double)need) * 6.0 / ballsLeft) : 0.0;
            info += String.format(Locale.getDefault(), " | RRR: %.2f", rrr);
            
            if (ballsLeft >= 0) {
                tvTarget.setText(String.format(Locale.getDefault(), "Target: %d | Need %d runs from %d balls", target, need, ballsLeft));
                tvTarget.setVisibility(View.VISIBLE);
            }
        } else {
            tvTarget.setVisibility(View.GONE);
        }
        
        tvInnings.setText(info);
        tvScore.setText(String.format(Locale.getDefault(), "%d/%d", score, wickets));
        tvOvers.setText(String.format(Locale.getDefault(), "Overs: %d.%d / %d", oversDone, ballsDone, match.getTotalOvers()));

        String sName = match.getStriker();
        String nsName = match.getNonStriker();
        String bName = match.getCurrentBowler();

        if (sName != null && !sName.isEmpty()) {
            int r = 0, b = 0;
            if (statsSnapshot != null) {
                DataSnapshot d = statsSnapshot.child("batsmen").child(sName);
                if (d.child("runs").getValue(Integer.class) != null) r = d.child("runs").getValue(Integer.class);
                if (d.child("balls").getValue(Integer.class) != null) b = d.child("balls").getValue(Integer.class);
            }
            tvStriker.setText(String.format(Locale.getDefault(), "%s: %d(%d)*", sName, r, b));
        } else {
            tvStriker.setText("—");
        }

        if (nsName != null && !nsName.isEmpty()) {
            int r = 0, b = 0;
            if (statsSnapshot != null) {
                DataSnapshot d = statsSnapshot.child("batsmen").child(nsName);
                if (d.child("runs").getValue(Integer.class) != null) r = d.child("runs").getValue(Integer.class);
                if (d.child("balls").getValue(Integer.class) != null) b = d.child("balls").getValue(Integer.class);
            }
            tvNonStriker.setText(String.format(Locale.getDefault(), "%s: %d(%d)", nsName, r, b));
        } else {
            tvNonStriker.setText("—");
        }

        if (bName != null && !bName.isEmpty()) {
            int r = 0, w = 0, b = 0;
            if (statsSnapshot != null) {
                DataSnapshot d = statsSnapshot.child("bowlers").child(bName);
                if (d.child("runs").getValue(Integer.class) != null) r = d.child("runs").getValue(Integer.class);
                if (d.child("wickets").getValue(Integer.class) != null) w = d.child("wickets").getValue(Integer.class);
                if (d.child("balls").getValue(Integer.class) != null) b = d.child("balls").getValue(Integer.class);
            }
            tvBowler.setText(String.format(Locale.getDefault(), "Bowler: %s %d-%d (%d.%d)", bName, w, r, b/6, b%6));
        } else {
            tvBowler.setText("Bowler: —");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchRef != null && scoreListener != null) {
            matchRef.removeEventListener(scoreListener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
