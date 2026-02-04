package com.example.castlegame.ui.game

import GamePhase
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle

import com.example.castlegame.data.model.League


data class GameUiState(
    val leagues: Map<League, List<CastleItem>> = emptyMap(),

    val currentLeague: League? = null,
    val completedLeagues: Set<League> = emptySet(),

    val internationalCastles: List<CastleItem> = emptyList(),
    val internationalWinner: CastleItem? = null,

    val currentPair: Pair<CastleItem, CastleItem>? = null,
    val remainingGames: Int = 0,

    val selectedIndex: Int? = null,
    val canProceed: Boolean = false,
    val leagueLocked: Boolean = false,

    val buttonText: String = "Select League",

    // 🧭 UI FLOW
    val phase: GamePhase = GamePhase.SELECT_LEAGUE,

    // 🏆 LIGA GYŐZTES
    val leagueWinner: CastleItem? = null,
    val superLeagueCastles: List<CastleItem> = emptyList(),
    // 🏆 SuperLeague
    val superLeagueWinner: CastleItem? = null,
    val globalRanking: List<GlobalCastle> = emptyList()


)


/*data class GameUiState(
    val completedLeagues: Set<League> = emptySet(),
    val currentLeague: League? = null,
    val leagueWinner: CastleItem? = null,

    val champions: Map<League, CastleItem> = emptyMap(),


    val leagues: Map<League, List<CastleItem>> = emptyMap(),
    val currentPair: Pair<CastleItem, CastleItem>? = null,

    val remainingGames: Int = 0,
    val selectedIndex: Int? = null,
    val canProceed: Boolean = false,
    val buttonText: String = "Select League",
    val leagueLocked: Boolean = false,
    val isLastPair: Boolean = false,
    val tapCounts: Map<String, Int> = emptyMap()
)*/

