# Cricket Match Tracker — Android App
### Mobile Application Development Mini Project

---

## 📁 Project Structure

```
CricketMatchTracker/
├── app/
│   ├── build.gradle
│   ├── google-services.json         ← REPLACE with your Firebase file
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/cricketmatchtracker/
│       │   ├── activities/
│       │   │   ├── HomeActivity.java
│       │   │   ├── OrganizerDashboardActivity.java
│       │   │   ├── CreateMatchActivity.java
│       │   │   ├── PreMatchSetupActivity.java
│       │   │   ├── OpeningPlayersActivity.java
│       │   │   ├── LiveScoringActivity.java
│       │   │   ├── ViewerDashboardActivity.java
│       │   │   ├── ViewerScoreActivity.java
│       │   │   └── MatchResultActivity.java
│       │   ├── adapters/
│       │   │   └── MatchAdapter.java
│       │   └── models/
│       │       └── Match.java
│       └── res/
│           ├── layout/
│           │   ├── activity_home.xml
│           │   ├── activity_organizer_dashboard.xml
│           │   ├── activity_viewer_dashboard.xml
│           │   ├── activity_create_match.xml
│           │   ├── activity_pre_match_setup.xml
│           │   ├── activity_opening_players.xml
│           │   ├── activity_live_scoring.xml
│           │   ├── activity_viewer_score.xml
│           │   ├── activity_match_result.xml
│           │   └── item_match.xml
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## ⚙️ SETUP INSTRUCTIONS

### Step 1 — Import into Android Studio
1. Open Android Studio
2. Click **File → Open**
3. Select the `CricketMatchTracker` folder
4. Wait for Gradle sync to finish

---

### Step 2 — Set Up Firebase

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **"Add project"** → give it a name (e.g., CricketTracker)
3. Disable Google Analytics (optional) → **Create project**

#### Add Android App to Firebase:
4. Click the **Android icon** on the project home page
5. Enter package name: `com.example.cricketmatchtracker`
6. Click **Register App**
7. Download `google-services.json`
8. **Replace** the placeholder `app/google-services.json` with this downloaded file

#### Enable Realtime Database:
9. In Firebase Console → **Build → Realtime Database**
10. Click **Create Database**
11. Choose location → Start in **Test mode** (allows read/write without auth)
12. Click **Enable**

---

### Step 3 — Build & Run
1. Connect an Android device or start an emulator (API 24+)
2. Click the **Run** button (▶) in Android Studio
3. Select your device and run

---

## 🎮 HOW TO USE THE APP

### As Organizer:
1. Open app → Select **Organizer**
2. Tap **+ Create New Match** → Enter team names and overs → Save
3. The match appears in the list with status **UPCOMING**
4. Tap the match → **Pre-Match Setup**:
   - Select toss winner and decision (Bat/Bowl)
   - Enter 11 player names for each team
   - Tap **Save & Proceed**
5. Select **Striker** and **Non-Striker** → **Start Match**
6. **Live Scoring Screen**:
   - Tap **0/1/2/3/4/6** to add runs
   - Tap **WD** for wide (no ball count)
   - Tap **NB** for no ball (no ball count)
   - Tap **OUT** for wicket → select next batsman
   - Tap **↩ Undo** to reverse last ball
7. After all overs or 10 wickets → Innings 2 begins
8. Match auto-completes and shows winner

### As Viewer:
1. Open app → Select **Viewer**
2. See all matches with color-coded status
3. Tap a **LIVE** match → See real-time score (auto-updates!)
4. Tap a **COMPLETED** match → See final result
5. Tap **UPCOMING** → Shows "Match not yet started"

---

## 🏗️ KEY TECHNICAL FEATURES

| Feature | Implementation |
|---|---|
| Real-time updates | Firebase `addValueEventListener` |
| Match list | RecyclerView + MatchAdapter |
| Scoring logic | Ball counter, over tracking, strike rotation |
| Wicket handling | AlertDialog for next batsman selection |
| Undo last ball | State saved before each action |
| Innings switch | Score/wickets reset, batting team swapped |
| Winner logic | Score comparison after innings 2 |

---

## 🗄️ Firebase Database Structure

```
matches
└── {matchId}
    ├── teamA: "Mumbai Indians"
    ├── teamB: "CSK"
    ├── totalOvers: 20
    ├── scoreA: 145
    ├── scoreB: 120
    ├── wicketsA: 4
    ├── wicketsB: 10
    ├── oversA: 20.0
    ├── oversB: 18.3
    ├── striker: "Rohit Sharma"
    ├── nonStriker: "Virat Kohli"
    ├── teamAPlayers: [...]
    ├── teamBPlayers: [...]
    ├── tossWinner: "Mumbai Indians"
    ├── tossDecision: "Bat"
    ├── currentInnings: 2
    ├── battingTeam: "B"
    ├── isStarted: true
    ├── isCompleted: false
    └── winner: null
```

---

## 📱 Screen Flow

```
HomeActivity
├── OrganizerDashboardActivity
│   ├── CreateMatchActivity
│   └── [tap match]
│       ├── PreMatchSetupActivity → OpeningPlayersActivity → LiveScoringActivity
│       ├── LiveScoringActivity (already started)
│       └── MatchResultActivity (completed)
└── ViewerDashboardActivity
    ├── ViewerScoreActivity (live)
    └── MatchResultActivity (completed)
```

---

## 🛠️ Tech Stack
- **Language**: Java
- **IDE**: Android Studio
- **Backend**: Firebase Realtime Database
- **UI**: Material Design Components, CardView, RecyclerView
- **Min SDK**: 24 (Android 7.0)
