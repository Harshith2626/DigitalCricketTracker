package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.adapters.MatchAdapter;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerDashboard";
    Button btnCreateMatch, btnClearMatches;
    RecyclerView recyclerView;
    MatchAdapter adapter;
    List<Match> matchList;
    DatabaseReference dbRef;
    TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Organizer Dashboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnCreateMatch = findViewById(R.id.btnCreateMatch);
        btnClearMatches = findViewById(R.id.btnClearMatches);
        recyclerView = findViewById(R.id.recyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        matchList = new ArrayList<>();
        adapter = new MatchAdapter(this, matchList, match -> {
            if (match.isCompleted()) {
                Intent intent = new Intent(this, MatchResultActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            } else if (match.isStarted()) {
                Intent intent = new Intent(this, LiveScoringActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            } else if (match.getTossWinner() != null && !match.getTossWinner().isEmpty()) {
                Intent intent = new Intent(this, OpeningPlayersActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, PreMatchSetupActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadLocalMatches();
        loadFirebaseMatches();

        btnCreateMatch.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateMatchActivity.class);
            startActivity(intent);
        });

        btnClearMatches.setOnClickListener(v -> showClearConfirmation());
    }

    private void showClearConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Match History")
                .setMessage("Are you sure you want to clear all locally saved matches?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    LocalDataManager.clearAllMatches(this);
                    matchList.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(this, "Local match history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadLocalMatches() {
        List<Match> localMatches = LocalDataManager.getAllMatches(this);
        if (!localMatches.isEmpty()) {
            matchList.clear();
            matchList.addAll(localMatches);
            sortAndNotify();
        }
    }

    private void loadFirebaseMatches() {
        dbRef = FirebaseDatabase.getInstance().getReference("matches");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Match> firebaseMatches = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            Match match = ds.getValue(Match.class);
                            if (match != null) {
                                match.setMatchId(ds.getKey());
                                firebaseMatches.add(match);
                                LocalDataManager.saveMatch(OrganizerDashboardActivity.this, match);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing match: " + e.getMessage());
                        }
                    }
                }
                mergeAndDisplay(firebaseMatches);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    private void mergeAndDisplay(List<Match> firebaseMatches) {
        Set<String> firebaseIds = new HashSet<>();
        for (Match m : firebaseMatches) firebaseIds.add(m.getMatchId());

        List<Match> combined = new ArrayList<>(firebaseMatches);
        List<Match> localMatches = LocalDataManager.getAllMatches(this);

        for (Match m : localMatches) {
            if (!firebaseIds.contains(m.getMatchId())) {
                combined.add(m);
            }
        }

        matchList.clear();
        matchList.addAll(combined);
        sortAndNotify();
    }

    private void sortAndNotify() {
        Collections.sort(matchList, (m1, m2) -> {
            int p1 = getPriority(m1);
            int p2 = getPriority(m2);
            return Integer.compare(p1, p2);
        });
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private int getPriority(Match m) {
        if (m.isStarted() && !m.isCompleted()) return 0; // LIVE
        if (!m.isStarted()) return 1; // UPCOMING
        return 2; // COMPLETED
    }

    private void updateEmptyState() {
        if (matchList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
