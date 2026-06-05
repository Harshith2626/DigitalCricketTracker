package com.example.cricketmatchtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.cricketmatchtracker.models.Match;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class LocalDataManager {
    private static final String PREF_NAME = "CricketMatchPrefs";
    private static final String KEY_MATCHES = "matches_list";

    public static void saveMatch(Context context, Match match) {
        List<Match> matches = getAllMatches(context);
        boolean updated = false;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).getMatchId().equals(match.getMatchId())) {
                matches.set(i, match);
                updated = true;
                break;
            }
        }
        if (!updated) {
            matches.add(0, match);
        }
        saveAllMatches(context, matches);
    }

    public static List<Match> getAllMatches(Context context) {
        List<Match> matches = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MATCHES, null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    Match m = jsonToMatch(array.getJSONObject(i));
                    if (m != null) matches.add(m);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return matches;
    }

    public static void clearAllMatches(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_MATCHES).apply();
    }

    public static Match getMatch(Context context, String matchId) {
        List<Match> matches = getAllMatches(context);
        for (Match m : matches) {
            if (m.getMatchId().equals(matchId)) return m;
        }
        return null;
    }

    private static void saveAllMatches(Context context, List<Match> matches) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (Match m : matches) {
            array.put(matchToJson(m));
        }
        prefs.edit().putString(KEY_MATCHES, array.toString()).apply();
    }

    private static JSONObject matchToJson(Match m) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("matchId", m.getMatchId());
            obj.put("teamA", m.getTeamA());
            obj.put("teamB", m.getTeamB());
            obj.put("totalOvers", m.getTotalOvers());
            obj.put("scoreA", m.getScoreA());
            obj.put("scoreB", m.getScoreB());
            obj.put("wicketsA", m.getWicketsA());
            obj.put("wicketsB", m.getWicketsB());
            obj.put("oversA", m.getOversA());
            obj.put("oversB", m.getOversB());
            obj.put("striker", m.getStriker());
            obj.put("nonStriker", m.getNonStriker());
            obj.put("currentBowler", m.getCurrentBowler());
            obj.put("battingTeam", m.getBattingTeam());
            obj.put("currentInnings", m.getCurrentInnings());
            obj.put("started", m.isStarted());
            obj.put("completed", m.isCompleted());
            obj.put("winner", m.getWinner());
            
            if (m.getTeamAPlayers() != null) obj.put("teamAPlayers", new JSONArray(m.getTeamAPlayers()));
            if (m.getTeamBPlayers() != null) obj.put("teamBPlayers", new JSONArray(m.getTeamBPlayers()));
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private static Match jsonToMatch(JSONObject obj) {
        try {
            Match m = new Match(obj.getString("teamA"), obj.getString("teamB"), obj.getInt("totalOvers"));
            m.setMatchId(obj.getString("matchId"));
            m.setScoreA(obj.optInt("scoreA", 0));
            m.setScoreB(obj.optInt("scoreB", 0));
            m.setWicketsA(obj.optInt("wicketsA", 0));
            m.setWicketsB(obj.optInt("wicketsB", 0));
            m.setOversA(obj.optDouble("oversA", 0.0));
            m.setOversB(obj.optDouble("oversB", 0.0));
            m.setStriker(obj.optString("striker", ""));
            m.setNonStriker(obj.optString("nonStriker", ""));
            m.setCurrentBowler(obj.optString("currentBowler", ""));
            m.setBattingTeam(obj.optString("battingTeam", "A"));
            m.setCurrentInnings(obj.optInt("currentInnings", 1));
            m.setStarted(obj.optBoolean("started", false));
            m.setCompleted(obj.optBoolean("completed", false));
            m.setWinner(obj.optString("winner", ""));

            JSONArray aPlayers = obj.optJSONArray("teamAPlayers");
            if (aPlayers != null) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < aPlayers.length(); i++) list.add(aPlayers.getString(i));
                m.setTeamAPlayers(list);
            }
            JSONArray bPlayers = obj.optJSONArray("teamBPlayers");
            if (bPlayers != null) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < bPlayers.length(); i++) list.add(bPlayers.getString(i));
                m.setTeamBPlayers(list);
            }
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
