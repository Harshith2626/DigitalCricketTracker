package com.example.cricketmatchtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.adapters.MatchAdapter;
import com.example.cricketmatchtracker.models.Match;
import com.example.cricketmatchtracker.utils.FirebaseUtil;
import com.example.cricketmatchtracker.utils.LocalDataManager;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ViewerDashboard";
    RecyclerView recyclerView;
    MatchAdapter adapter;
    List<Match> matchList;
    DatabaseReference dbRef;
    TextView tvEmptyState;
    ProgressBar progressBar;
    
    private ValueEventListener matchListener;
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable = () -> {
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.setVisibility(View.GONE);
            if (matchList.isEmpty()) {
                tvEmptyState.setText("Taking too long to load. Check your connection.");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Live Matches");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        
        matchList = new ArrayList<>();
        adapter = new MatchAdapter(this, matchList, match -> {
            if (match.isCompleted()) {
                Intent intent = new Intent(this, MatchResultActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            } else if (match.isStarted()) {
                Intent intent = new Intent(this, ViewerScoreActivity.class);
                intent.putExtra("matchId", match.getMatchId());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Match hasn't started yet", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadLocalMatches();
        loadFirebaseMatches();
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
        progressBar.setVisibility(View.VISIBLE);
        timeoutHandler.postDelayed(timeoutRunnable, 7000);

        try {
            dbRef = FirebaseUtil.getDatabase().getReference("matches");
            matchListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    progressBar.setVisibility(View.GONE);
                    List<Match> firebaseMatches = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Match m = ds.getValue(Match.class);
                                if (m != null) {
                                    m.setMatchId(ds.getKey());
                                    firebaseMatches.add(m);
                                    LocalDataManager.saveMatch(ViewerDashboardActivity.this, m);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error: " + e.getMessage());
                            }
                        }
                    }
                    mergeAndDisplay(firebaseMatches);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Firebase error: " + error.getMessage());
                    updateEmptyState();
                }
            };
            dbRef.addValueEventListener(matchListener);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
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
            int p1 = (m1.isStarted() && !m1.isCompleted()) ? 0 : (!m1.isStarted() ? 1 : 2);
            int p2 = (m2.isStarted() && !m2.isCompleted()) ? 0 : (!m2.isStarted() ? 1 : 2);
            return Integer.compare(p1, p2);
        });
        adapter.notifyDataSetChanged();
        updateEmptyState();
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
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (dbRef != null && matchListener != null) {
            dbRef.removeEventListener(matchListener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
