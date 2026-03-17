package com.example.castlegame.ui.game

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
    val globalRanking: List<GlobalCastle> = emptyList(),

    val userSuperLeagueRanking: List<Pair<CastleItem, Int>> = emptyList(),  // Update type

    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    val infoMessage: String? = null,

    val castleForInfo: CastleItem? = null,

    val availableCountries: List<String> = emptyList(),   // populated after data loads
    val currentCountry: String? = null,                   // the country being played
    val countryWinner: CastleItem? = null,                 // winner of the country tournament


    val playedCountries: Set<String> = emptySet(),
    val userCountryRanking: List<Pair<CastleItem, Int>> = emptyList(),
    val globalCountryRanking: List<GlobalCastle> = emptyList(),
    val isCountryRankingLoading: Boolean = false,

    // 🆕 Personal league flow
    val allCountriesPlayed: Boolean = false,
    val userLeagueCastles: Map<League, List<CastleItem>> = emptyMap(),
    val userLeagueWinner: CastleItem? = null,
    val userLeagueCompletedLeagues: Set<League> = emptySet(),   // tracks which user leagues are done

    val userLeagueTopResults: Map<League, CastleItem> = emptyMap(),     // winner per league → feeds personal superleague
    val userPersonalSuperLeagueCastles: List<CastleItem> = emptyList(),
    val userPersonalSuperLeagueWinner: CastleItem? = null,
    val userPersonalSuperLeagueRanking: List<Pair<CastleItem, Int>> = emptyList(),


)
