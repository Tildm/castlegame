package com.example.castlegame.ui.game

enum class GamePhase {
    SELECT_LEAGUE,

    // ── Standard league flow ─────────────────────────────────────────────────
    PLAYING,
    LEAGUE_WINNER,
    LEAGUE_RANKING,

    // ── Personal league flow ─────────────────────────────────────────────────
    USER_LEAGUE_PLAYING,
    USER_LEAGUE_WINNER,
    USER_LEAGUE_RANKING,

    // ── SuperLeague flow ─────────────────────────────────────────────────────
    SUPERLEAGUE_PLAYING,
    SUPERLEAGUE_WINNER,
    SUPERLEAGUE_RANKING,
    USER_SUPERLEAGUE_RANKING,

    // ── Country tournament flow ──────────────────────────────────────────────
    COUNTRY_PLAYING,
    COUNTRY_WINNER,
    COUNTRY_RANKING,
    USER_COUNTRY_RANKING,

    // ── Personal SuperLeague flow ────────────────────────────────────────────
    USER_PERSONAL_SUPERLEAGUE_PLAYING,
    USER_PERSONAL_SUPERLEAGUE_WINNER,
    USER_PERSONAL_SUPERLEAGUE_RANKING,

    // ── Utility ──────────────────────────────────────────────────────────────
    CASTLE_INFO,

    // ── Quiz ──────────────────────────────────────────────────────────────
    QUIZ,
    QUIZ_SUMMARY,
}

/*
package com.example.castlegame.ui.game

enum class GamePhase {
    SELECT_LEAGUE,
    PLAYING,
    LEAGUE_WINNER,
    LEAGUE_RANKING,
    // 🆕 Personal league flow
    USER_LEAGUE_PLAYING,
    USER_LEAGUE_WINNER,
    USER_LEAGUE_RANKING,
    SUPERLEAGUE_PLAYING,   // 🆕 8 vár egymás ellen
    SUPERLEAGUE_WINNER,
    SUPERLEAGUE_RANKING,
   USER_SUPERLEAGUE_RANKING,
    CASTLE_INFO,
    COUNTRY_PLAYING,    // 🆕 Country tournament in progress
    COUNTRY_WINNER,
    COUNTRY_RANKING,
    USER_COUNTRY_RANKING,

    //User Personal league flow
    USER_PERSONAL_SUPERLEAGUE_PLAYING,
    USER_PERSONAL_SUPERLEAGUE_WINNER,
    USER_PERSONAL_SUPERLEAGUE_RANKING,

}

*/
