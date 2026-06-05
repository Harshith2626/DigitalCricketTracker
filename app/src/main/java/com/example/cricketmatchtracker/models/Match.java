package com.example.cricketmatchtracker.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Match implements Serializable {
    private String matchId;
    private String teamA;
    private String teamB;
    private int totalOvers;
    private int scoreA;
    private int scoreB;
    private int wicketsA;
    private int wicketsB;
    private double oversA;
    private double oversB;
    private String striker;
    private String nonStriker;
    private String currentBowler;
    private List<String> teamAPlayers;
    private List<String> teamBPlayers;
    private String tossWinner;
    private String tossDecision;
    private String battingTeam;
    private int currentInnings;
    private boolean started;
    private boolean completed;
    private String winner;

    // Required for Firebase
    public Match() {
        this.teamAPlayers = new ArrayList<>();
        this.teamBPlayers = new ArrayList<>();
        this.currentInnings = 1;
        this.battingTeam = "A";
        this.scoreA = 0;
        this.scoreB = 0;
        this.wicketsA = 0;
        this.wicketsB = 0;
        this.oversA = 0.0;
        this.oversB = 0.0;
    }

    public Match(String teamA, String teamB, int totalOvers) {
        this();
        this.teamA = teamA;
        this.teamB = teamB;
        this.totalOvers = totalOvers;
        this.started = false;
        this.completed = false;
    }

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getTeamA() { return teamA; }
    public void setTeamA(String teamA) { this.teamA = teamA; }

    public String getTeamB() { return teamB; }
    public void setTeamB(String teamB) { this.teamB = teamB; }

    public int getTotalOvers() { return totalOvers; }
    public void setTotalOvers(int totalOvers) { this.totalOvers = totalOvers; }

    public int getScoreA() { return scoreA; }
    public void setScoreA(int scoreA) { this.scoreA = scoreA; }

    public int getScoreB() { return scoreB; }
    public void setScoreB(int scoreB) { this.scoreB = scoreB; }

    public int getWicketsA() { return wicketsA; }
    public void setWicketsA(int wicketsA) { this.wicketsA = wicketsA; }

    public int getWicketsB() { return wicketsB; }
    public void setWicketsB(int wicketsB) { this.wicketsB = wicketsB; }

    public double getOversA() { return oversA; }
    public void setOversA(double oversA) { this.oversA = oversA; }

    public double getOversB() { return oversB; }
    public void setOversB(double oversB) { this.oversB = oversB; }

    public String getStriker() { return striker; }
    public void setStriker(String striker) { this.striker = striker; }

    public String getNonStriker() { return nonStriker; }
    public void setNonStriker(String nonStriker) { this.nonStriker = nonStriker; }

    public String getCurrentBowler() { return currentBowler; }
    public void setCurrentBowler(String currentBowler) { this.currentBowler = currentBowler; }

    public List<String> getTeamAPlayers() { return teamAPlayers; }
    public void setTeamAPlayers(List<String> teamAPlayers) { this.teamAPlayers = teamAPlayers; }

    public List<String> getTeamBPlayers() { return teamBPlayers; }
    public void setTeamBPlayers(List<String> teamBPlayers) { this.teamBPlayers = teamBPlayers; }

    public String getTossWinner() { return tossWinner; }
    public void setTossWinner(String tossWinner) { this.tossWinner = tossWinner; }

    public String getTossDecision() { return tossDecision; }
    public void setTossDecision(String tossDecision) { this.tossDecision = tossDecision; }

    public String getBattingTeam() { return battingTeam; }
    public void setBattingTeam(String battingTeam) { this.battingTeam = battingTeam; }

    public int getCurrentInnings() { return currentInnings; }
    public void setCurrentInnings(int currentInnings) { this.currentInnings = currentInnings; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public String getStatus() {
        if (completed) return "COMPLETED";
        if (started) return "LIVE";
        return "UPCOMING";
    }
}
