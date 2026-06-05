package com.example.cricketmatchtracker.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cricketmatchtracker.R;
import com.example.cricketmatchtracker.models.Match;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private Context context;
    private List<Match> matchList;
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(Match match);
    }

    public MatchAdapter(Context context, List<Match> matchList, OnMatchClickListener listener) {
        this.context = context;
        this.matchList = matchList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matchList.get(position);

        holder.tvTeams.setText(match.getTeamA() + " vs " + match.getTeamB());
        holder.tvOvers.setText("Total Overs: " + match.getTotalOvers());

        if (match.isCompleted()) {
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.setTextColor(Color.parseColor("#757575")); // Gray

            String winner = match.getWinner();
            if (winner != null && !winner.isEmpty()) {
                if ("Tie".equals(winner)) {
                    holder.tvScore.setText("Result: Match Tied");
                } else {
                    holder.tvScore.setText("Result: " + winner + " won");
                }
            } else {
                holder.tvScore.setText(match.getScoreA() + "/" + match.getWicketsA()
                        + " vs " + match.getScoreB() + "/" + match.getWicketsB());
            }
        } else if (match.isStarted()) {
            holder.tvStatus.setText("● LIVE");
            holder.tvStatus.setTextColor(Color.parseColor("#D32F2F")); // Red

            holder.tvScore.setText(match.getScoreA() + "/" + match.getWicketsA()
                    + "  |  " + match.getScoreB() + "/" + match.getWicketsB());
        } else {
            holder.tvStatus.setText("UPCOMING");
            holder.tvStatus.setTextColor(Color.parseColor("#1976D2")); // Blue
            holder.tvScore.setText("Match not started yet");
        }

        holder.cardView.setOnClickListener(v -> listener.onMatchClick(match));
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }

    public void setList(List<Match> newList) {
        this.matchList = newList;
        notifyDataSetChanged();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTeams, tvStatus, tvScore, tvOvers;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTeams = itemView.findViewById(R.id.tvTeams);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvOvers = itemView.findViewById(R.id.tvOvers);
        }
    }
}
