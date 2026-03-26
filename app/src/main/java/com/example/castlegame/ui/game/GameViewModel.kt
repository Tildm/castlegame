package com.example.castlegame.ui.game


import LeagueResult
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.castlegame.data.model.ApiCastle
import com.example.castlegame.data.model.ApiImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.data.model.League
import com.example.castlegame.data.remote.NetworkModule
import com.example.castlegame.data.repository.GlobalRepository
import com.example.castlegame.data.repository.LeagueRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class GameViewModel(application: Application) : AndroidViewModel(application) {

    init {
        Log.d("GameViewModel", "CREATED")
    }

    private val globalRepository: GlobalRepository = GlobalRepository()

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private val repository = LeagueRepository()
    private val tapCounts = mutableMapOf<String, Int>()
    private val champions = mutableMapOf<League, CastleItem>()
    private val _uiState = MutableStateFlow(GameUiState())
    var uiState: StateFlow<GameUiState> = _uiState
    private var shuffledPairs: MutableList<Pair<CastleItem, CastleItem>> =
        mutableListOf()
    private val winCounts = mutableMapOf<String, Int>()

    private val leagueTopResults = mutableMapOf<League, List<CastleItem>>()

    private var lastRankingPhase: GamePhase? = null

    var selectedCastleForInfo: CastleItem? = null
        private set

    var totalGames: Int = 0

    private val headToHead = mutableMapOf<Pair<String, String>, String>() //egymás elleni meccs eredményének tárolásához

    // 🌍 All castles across all leagues, used to build country lists
    private var allCastles: List<CastleItem> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val leagues = repository.loadLeagues()
            val startLeague = League.WEST
            val pairs = leagues[startLeague].orEmpty()

            // 🌍 Collect all castles and extract unique country list
            //allCastles = leagues.values.flatten()
            allCastles = repository.loadAllCastles()
            val countries = allCastles
                .map { it.country }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            _uiState.update {
                it.copy(
                    leagues = leagues,
                    currentLeague = startLeague,
                    availableCountries = countries
                )
            }

            // ✅ Promote last week's country winners into their leagues (runs once per week)
            promoteCountryWinnersIfNewWeek(countries)

            loadPlayedCountries()

            resetGame(pairs)
        }
    }

    private fun promoteCountryWinnersIfNewWeek(countries: List<String>) {
        val calendar = java.util.Calendar.getInstance()
        val year  = calendar.get(java.util.Calendar.YEAR)
        val week  = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val weekKey = "$year-W${week.toString().padStart(2, '0')}"

        val prefs = getApplication<Application>().getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val lastPromotedWeek = prefs.getString("last_promoted_week", "")

        if (lastPromotedWeek == weekKey) {
            Log.d("GameViewModel", "Already promoted for week $weekKey, skipping")
            return
        }

        repository.promoteCountryWinnersToLeagues(
            weekKey = weekKey,
            countries = countries,
            onSuccess = {
                prefs.edit().putString("last_promoted_week", weekKey).apply()
                Log.d("GameViewModel", "Country winners promoted for week $weekKey")
            },
            onError = {
                Log.e("GameViewModel", "Promotion failed", it)
            }
        )
    }


    fun selectLeague(league: League) {
       // Log.d("GameViewModel", "selectLeague: $league")

        // Clear win counts for the new league
        winCounts.clear()

        val castles = _uiState.value.leagues[league] ?: return

        shuffledPairs = generateAllPairs(castles).shuffled().toMutableList()
        totalGames = shuffledPairs.size

        val firstPair = if (shuffledPairs.isNotEmpty()) {
            shuffledPairs.removeAt(0)
        } else null

        _uiState.update { currentState ->
            currentState.copy(
                currentLeague = league,
                phase = GamePhase.PLAYING,
                currentPair = firstPair,
                remainingGames = totalGames,
                selectedIndex = null,
                leagueLocked = true
            )
        }
    }

    // 🌍 Start a country tournament — mirrors selectLeague() exactly
    fun selectCountry(country: String) {
        winCounts.clear()
        headToHead.clear()

        Log.d("GameViewModel", "country tournament")

        val castles = allCastles.filter { it.country == country }
        Log.d("GameViewModel", "country tournament - all castles size= ${allCastles.size}")
        Log.d("GameViewModel", "country tournament - castles size= ${castles}")
        if (castles.size < 2) {
            _uiState.update { it.copy(infoMessage = "Not enough castles for $country tournament.") }
            return
        }

        shuffledPairs = generateAllPairs(castles).shuffled().toMutableList()

        totalGames = shuffledPairs.size

        val firstPair = if (shuffledPairs.isNotEmpty()) {
            shuffledPairs.removeAt(0)
        } else null

        _uiState.update { state ->
            state.copy(
                currentCountry = country,
                phase = GamePhase.COUNTRY_PLAYING,
                currentPair = firstPair,
                remainingGames = totalGames,
                selectedIndex = null,
                leagueLocked = true,
                countryWinner = null
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onCastleSelected(index: Int) {

        Log.d("GameViewModel", "onCastleSelected START, index=$index")

        val currentPhase = _uiState.value.phase

        if (currentPhase != GamePhase.PLAYING &&
            currentPhase != GamePhase.USER_LEAGUE_PLAYING &&
            currentPhase != GamePhase.SUPERLEAGUE_PLAYING &&
            currentPhase != GamePhase.COUNTRY_PLAYING &&
            currentPhase != GamePhase.USER_PERSONAL_SUPERLEAGUE_PLAYING// 🆕
        ) {
         //   Log.d("GameViewModel", "onCastleSelected IGNORED, phase=$currentPhase")
            return
        }

        _uiState.update { it.copy(selectedIndex = index) }

        viewModelScope.launch {
            delay(300)

            val pair = _uiState.value.currentPair ?: return@launch
            val winner = if (index == 0) pair.first else pair.second

            winCounts[winner.id] = (winCounts[winner.id] ?: 0) + 1

            val key = listOf(pair.first.id, pair.second.id).sorted()
            headToHead[key[0] to key[1]] = winner.id

           Log.d("GameViewModel", "shuffledPairs in CastleSelected = $shuffledPairs")

            if (shuffledPairs.isEmpty()) {
                Log.d("GameViewModel", "if shuffledPairs empty in CastleSelected")

                when (currentPhase) {
                    GamePhase.PLAYING -> finishLeague()
                    GamePhase.SUPERLEAGUE_PLAYING -> finishSuperLeague()
                    GamePhase.COUNTRY_PLAYING -> finishCountryTournament()  // 🆕
                    GamePhase.USER_LEAGUE_PLAYING -> finishUserLeague()
                    GamePhase.USER_PERSONAL_SUPERLEAGUE_PLAYING -> finishUserPersonalSuperLeague()
                    else -> {}
                }
            } else {
                nextPair()
            }

            //Log.d("GameViewModel", "onCastleSelected END")
        }
    }



    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun nextPair() {
       // Log.d("GameViewModel", "nextPair() called, shuffled size = ${shuffledPairs.size}")

        val phase = _uiState.value.phase

        if (
            phase != GamePhase.PLAYING &&
            phase != GamePhase.SUPERLEAGUE_PLAYING &&
            phase != GamePhase.COUNTRY_PLAYING &&
            phase != GamePhase.USER_LEAGUE_PLAYING &&
            phase != GamePhase.USER_PERSONAL_SUPERLEAGUE_PLAYING// 🆕
        ) {
           // Log.d("GameViewModel", "nextPair SKIPPED, phase=$phase")
            return
        }

        if (shuffledPairs.isEmpty()) {
            // Route to the correct finish function based on current phase
            when (phase) {
                GamePhase.PLAYING               -> finishLeague()
                GamePhase.SUPERLEAGUE_PLAYING   -> finishSuperLeague()
                GamePhase.COUNTRY_PLAYING       -> finishCountryTournament()
                GamePhase.USER_LEAGUE_PLAYING   -> finishUserLeague()   // 🆕
                GamePhase.USER_PERSONAL_SUPERLEAGUE_PLAYING -> finishUserPersonalSuperLeague()
                else -> {}
            }
            return
        }

        val next = shuffledPairs.removeFirst()

        _uiState.update {
            it.copy(
                currentPair = next,
                remainingGames = shuffledPairs.size + 1,
                leagueLocked = true,
                selectedIndex = null  // ✅ Reset selection for new pair
            )
        }
    }



    private fun resetGame(castleItem: List<CastleItem>) {
        tapCounts.clear()

        val shuffled = generatePairs(castleItem).toMutableList()

        _uiState.update {
            it.copy(
                currentPair = shuffled.removeFirstOrNull(),
                //remainingGames = shuffled.size,
                selectedIndex = null,
                canProceed = false,
                //leagueLocked = false,
                buttonText = "Next",
                remainingGames = 0


            )
        }

        shuffledPairs = shuffled
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun finishLeague() {
        val state = _uiState.value
        val league = _uiState.value.currentLeague ?: return
        val updated = _uiState.value.completedLeagues + league

       // Log.d("GameViewModel", "finishLeague CALLED")
      //  Log.d("GameViewModel", "Current phase BEFORE update: ${state.phase}")
      //  Log.d("GameViewModel", "league castles = ${_uiState.value.leagues[league]}")
       // Log.d("GameViewModel", "winCounts = $winCounts")  // ← ADD THIS DEBUG

        // ✅ FIX: Use winCounts instead of tapCounts
        val winnerId = winCounts.maxByOrNull { it.value }?.key
        val winner = state.leagues[league]
            ?.firstOrNull { it.id == winnerId }

        Log.d("GameViewModel", "winner = $winner")
        Log.d("GameViewModel", "winnerId = $winnerId")

        // Store the winner
        if (winner != null) {
            champions[league] = winner
        }

        // ✅ FIX: Use winCounts for ranking
        val ranking = winCounts
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val result = LeagueResult(
            league = league,
            scores = winCounts.toMap(),  // ✅ FIX: Use winCounts
            ranking = ranking
        )

        val uid = userId

        if (uid != null && winner != null) {
            repository.saveLeagueResult(
                userId = uid,
                leagueId = league.name,
                winner = winner,
                allResults = getLeagueRanking(league),  // ← add this
                onSuccess = { Log.d("LeagueRepo", "Saved") },
                onError = { Log.e("LeagueRepo", "Failed", it) }
            )
        }

        // 🥇🥈 TOP 2 selection - ✅ FIX: Use winCounts
        val top2Ids = winCounts
            .toList()
            .sortedByDescending { it.second }
            .take(2)
            .map { it.first }

        val top2Castles = state.leagues[league]
            ?.filter { it.id in top2Ids }
            ?: emptyList()

        leagueTopResults[league] = top2Castles

        //Log.d("GameViewModel", "TOP2 for ${league.name}: ${top2Castles.map { it.title }}")
       // Log.d("GameViewModel", "TOP2 for ${league.name}: $top2Castles")

        _uiState.update {
            it.copy(
                leagueWinner = winner,
                phase = GamePhase.LEAGUE_WINNER,
                completedLeagues = it.completedLeagues + league,
                currentLeague = league,
                currentPair = null,
                leagueLocked = false,
                selectedIndex = null,
                canProceed = false,
                remainingGames = 0,
                buttonText = if (updated.size == League.entries.size)
                    "Show Results"
                else
                    "Select League"
            )
        }

       // Log.d("GameViewModel", "Phase AFTER update: ${_uiState.value.phase}")
      //  Log.d("GameViewModel", "leagueWinner AFTER update: ${_uiState.value.leagueWinner}")
    }


    // 🌍 Finish country tournament — shows winner then goes back to SELECT_LEAGUE
    private fun finishCountryTournament() {
        val country = _uiState.value.currentCountry ?: return
        val castles = allCastles.filter { it.country == country }

        val winnerId = winCounts.maxByOrNull { it.value }?.key
        val winner = castles.firstOrNull { it.id == winnerId }

        Log.d("GameViewModel", "Country winner = $winner")

        // ✅ Save country result — mirrors league saving
        val uid = userId
        if (uid != null && winner != null) {
            repository.saveCountryResult(
               // userId = uid,
                country = country,
              //  winner = winner,
                allResults = getCountryRanking(),
                onSuccess = { Log.d("LeagueRepo", "Country $country saved") },
                onError = { Log.e("LeagueRepo", "Failed to save country $country", it) }
            )
        }
        repository.saveUserCountryRanking(
        country    = country,
        allResults = getCountryRanking(),
        onSuccess  = {
            // refresh the played-countries set so the drawer updates immediately
            loadPlayedCountries()
        }
    )
        _uiState.update {
            it.copy(
                countryWinner = winner,
                phase = GamePhase.COUNTRY_WINNER,
                currentPair = null,
                leagueLocked = false,
                selectedIndex = null,
                remainingGames = 0
            )
        }
    }


    // 🌍 Build ranking for the country that was just played
    fun getCountryRanking(): List<Pair<CastleItem, Int>> {
        val country = _uiState.value.currentCountry ?: return emptyList()
        val castles = allCastles.filter { it.country == country }

        return castles
            .map { castle -> castle to (winCounts[castle.id] ?: 0) }
            .sortedWith(
                compareByDescending<Pair<CastleItem, Int>> { it.second }
                    .thenComparator { a, b ->
                        if (a.second == b.second) {
                            val key = listOf(a.first.id, b.first.id).sorted()
                            val winnerId = headToHead[key[0] to key[1]]
                            when (winnerId) {
                                a.first.id -> -1
                                b.first.id -> 1
                                else -> 0
                            }
                        } else 0
                    }
            )
    }

    // 🌍 After viewing country ranking, return to main menu
    fun continueFromCountryRanking() {
        winCounts.clear()
        headToHead.clear()

        _uiState.update {
            it.copy(
                phase = GamePhase.SELECT_LEAGUE,
                currentCountry = null,
                countryWinner = null,
                currentPair = null,
                selectedIndex = null,
                leagueLocked = false
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun continueFromWinner() {
        // Always show ranking first
        _uiState.update {
            it.copy(
                leagueWinner = null,
                currentPair = null,
                phase = GamePhase.LEAGUE_RANKING
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadGlobalCountryRankingFromCache(
        country: String,
        onSuccess: (List<GlobalCastle>) -> Unit
    ) {

        Log.d("CountryRanking", "=== loadGlobalCountryRankingFromCache START ===")
        Log.d("CountryRanking", "country = $country")
        Log.d("CountryRanking", "allCastles.size = ${allCastles.size}")

        if (allCastles.isEmpty()) {
            Log.e("CountryRanking", "allCastles is EMPTY — data may not have loaded yet!")
            _uiState.update { it.copy(isCountryRankingLoading = false) }
            return
        }
        // Build a fake ApiCastle list from already-loaded CastleItem data
        // so GlobalRepository can enrich Firestore win counts without a network call.
        val fakeApiCastles = allCastles.map { castle ->
            ApiCastle(
                id = castle.id,
                title = castle.title,
                group = "",                 // not needed for ranking enrichment
                image = castle.imageUrl.map { ApiImage(url = it) },
                description = castle.description,
                wikiUrl = castle.wikiUrl,
                country = castle.country,
                text = "",
                built = castle.built,
                style = castle.style,
                visiting = castle.visiting,
                location = castle.location
            )
        }

        Log.d("CountryRanking", "fakeApiCastles built, size = ${fakeApiCastles.size}")
        Log.d("CountryRanking", "Calling globalRepository.loadGlobalCountryRanking...")

        globalRepository.loadGlobalCountryRanking(
            country    = country,
            apiCastles = fakeApiCastles,
            onSuccess  = { list ->
                Log.d("CountryRanking", "onSuccess — list.size = ${list.size}")
                onSuccess(list)
            },
            onError    = { e ->
                Log.e("GameViewModel", "Failed to load global country ranking", e)
                _uiState.update { it.copy(isCountryRankingLoading = false) }
            }
        )
        Log.d("CountryRanking", "=== loadGlobalCountryRankingFromCache END (async continues) ===")

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun continueFromCountryWinner() {
        val country = _uiState.value.currentCountry ?: return

        _uiState.update {
            it.copy(
                phase                   = GamePhase.COUNTRY_RANKING,
                currentPair             = null,
                selectedIndex           = null,
                isCountryRankingLoading = true,
                globalCountryRanking    = emptyList()
            )
        }

        loadGlobalCountryRankingFromCache(country) { list ->
            _uiState.update {
                it.copy(
                    globalCountryRanking    = list,
                    isCountryRankingLoading = false
                )
            }
        }
    }

// ── (b) New: randomNextCountry() ────────────────────────────────
//    Picks a random country that has NOT been played yet, then
//    starts it.  If all countries have been played, wraps around.

    fun randomNextCountry() {
        val state     = _uiState.value
        val available = state.availableCountries
        val played    = state.playedCountries

        val unplayed  = available.filter { it !in played }
        val pool      = unplayed.ifEmpty { available }   // wrap-around when all played

        if (pool.isEmpty()) return

        val next = pool.random()
        selectCountry(next)
    }


// ── (c) New: goToGlobalCountryRanking() ─────────────────────────
//    From UserCountryRankingScreen → switch to COUNTRY_RANKING
//    and load the global data for the current country.

    @RequiresApi(Build.VERSION_CODES.O)
    fun goToGlobalCountryRanking() {
        val country = _uiState.value.currentCountry ?: return

        _uiState.update {
            it.copy(
                phase                   = GamePhase.COUNTRY_RANKING,
                isCountryRankingLoading = true,
                globalCountryRanking    = emptyList()
            )
        }

        loadGlobalCountryRankingFromCache(country) { list ->
            _uiState.update {
                it.copy(
                    globalCountryRanking    = list,
                    isCountryRankingLoading = false
                )
            }
        }
    }

// ── (d) New: goToUserCountryRanking() ───────────────────────────
//    From CountryRankingScreen → switch to USER_COUNTRY_RANKING
//    and load the saved ranking for the current country.

    fun goToUserCountryRanking() {
        val country = _uiState.value.currentCountry ?: return

        viewModelScope.launch {
            val savedRanking = repository.loadUserCountryRanking(country)
            if (savedRanking != null) {
                _uiState.update {
                    it.copy(
                        userCountryRanking = savedRanking,
                        phase = GamePhase.USER_COUNTRY_RANKING
                    )
                }
            }
            // If no saved ranking yet, stay on CountryRankingScreen (nothing to show)
        }
    }



    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun continueFromRanking() {
        val state     = _uiState.value
        val completed = state.completedLeagues
        val total     = League.entries.size

        if (completed.size == total) {
            // All four leagues done → start SuperLeague
            startSuperLeague()
        } else {
            // Auto-advance to the next unplayed league in declaration order
            tapCounts.clear()
            winCounts.clear()
            headToHead.clear()

            val nextLeague = League.entries.firstOrNull { it !in completed }

            if (nextLeague != null) {
                // Start it immediately — no manual selection needed
                selectLeague(nextLeague)
            } else {
                // Fallback — should never happen, but stay safe
                _uiState.update {
                    it.copy(
                        currentLeague = null,
                        leagueWinner  = null,
                        phase         = GamePhase.SELECT_LEAGUE,
                        buttonText    = "Select League"
                    )
                }
            }
        }
    }


    /*    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        fun continueFromRanking() {
            val completed = _uiState.value.completedLeagues.size
            val total = League.entries.size

            if (completed == total) {
                // All leagues done → Start SuperLeague
             //   Log.d("GameViewModel", "All leagues completed, starting SuperLeague")
                startSuperLeague()
            } else {
                // More leagues to play → Back to league selection
                tapCounts.clear()

                _uiState.update {
                    it.copy(
                        currentLeague = null,
                        leagueWinner = null,
                        phase = GamePhase.SELECT_LEAGUE,
                        buttonText = "Select League"
                    )
                }
            }
        }*/



    private fun generatePairs(
        list: List<CastleItem>
    ): List<Pair<CastleItem, CastleItem>> =
        list.flatMapIndexed { index, item ->
            list.drop(index + 1).map { item to it }
        }.shuffled()


    fun getLeagueRanking(league: League): List<Pair<CastleItem, Int>> {
        val items = uiState.value.leagues[league].orEmpty()

        return items
            .map { castle ->
                castle to (winCounts[castle.id] ?: 0)
            }
            .sortedWith(
                compareByDescending<Pair<CastleItem, Int>> { it.second }
                    .thenComparator { a, b ->

                        // csak akkor vizsgáljuk ha azonos a pont
                        if (a.second == b.second) {

                            val key = listOf(a.first.id, b.first.id).sorted()
                            val winnerId = headToHead[key[0] to key[1]]

                            when (winnerId) {
                                a.first.id -> -1  // a előrébb
                                b.first.id -> 1   // b előrébb
                                else -> 0
                            }
                        } else {
                            0
                        }
                    }
            )
    }



    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun startSuperLeague() {
        Log.d("GameViewModel", "SuperLeague here starts")

        // ✅ Use leagueTopResults which was populated correctly after each league finished
        // instead of getLeagueRanking() which only reads the current winCounts (last league only)
        val top2PerLeague = mutableListOf<CastleItem>()
        League.entries.forEach { league ->
            val top2 = leagueTopResults[league] ?: emptyList()
            top2PerLeague.addAll(top2)
        }

        winCounts.clear()

        shuffledPairs = generateAllPairs(top2PerLeague).shuffled().toMutableList()
        totalGames = shuffledPairs.size
        Log.d("GameViewModel", "totalGames = $totalGames")

        // ✅ Get first pair here, don't call nextPair() separately (it ran on stale state)
        val firstPair = if (shuffledPairs.isNotEmpty()) {
            shuffledPairs.removeAt(0)
        } else null

        _uiState.update { state ->
            state.copy(
                phase = GamePhase.SUPERLEAGUE_PLAYING,
                superLeagueCastles = top2PerLeague,
                currentLeague = null,
                leagueWinner = null,
                selectedIndex = null,
                currentPair = firstPair,
                remainingGames = totalGames,
            )
        }
    }


    private fun generateAllPairs(castles: List<CastleItem>): List<Pair<CastleItem, CastleItem>> {
        val pairs = mutableListOf<Pair<CastleItem, CastleItem>>()

        for (i in castles.indices) {
            for (j in i + 1 until castles.size) {
                pairs.add(castles[i] to castles[j])
            }
        }

        return pairs
    }



    private fun finishSuperLeague() {
        Log.d("GameViewModel", "finishSuperLeague CALLED")

        val superLeagueCastles = _uiState.value.superLeagueCastles

        // Find the winner (castle with most wins)
        val winner = superLeagueCastles.maxByOrNull { castle ->
            winCounts[castle.id] ?: 0
        }

        Log.d("GameViewModel", "SuperLeague winner = $winner")

        // Create global ranking
        val globalRanking = superLeagueCastles.map { castle ->
            GlobalCastle(
                id = castle.id,
                title = castle.title,
                imageUrl = castle.imageUrl,
                wins = winCounts[castle.id] ?: 0,
                description = castle.description,
                visiting = castle.visiting,
                wikiUrl = castle.wikiUrl,
                country = castle.country,
                location = castle.location,
                style = castle.style,
                built = castle.built,
            )
        }.sortedByDescending { it.wins }



        _uiState.update { state ->
            state.copy(
                phase = GamePhase.SUPERLEAGUE_WINNER,
                superLeagueWinner = winner,
                globalRanking = globalRanking,
                currentPair = null,
                selectedIndex = null,
                remainingGames = 0
            )
        }

        globalRepository.saveSuperLeagueResultsWithHistory(
            userId = userId,
            results = globalRanking,
            onError = { e ->
                Log.w(
                    "Firestore",
                    "Saved locally, will sync later (offline or permission issue)",
                    e
                )
            }
        )

        Log.d("GameViewModel", "globalRanking = $globalRanking")


    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun continueFromSuperLeagueWinner() {
        Log.d("GameViewModel", "Continue from SUPERLEAGUE_WINNER")

        _uiState.update {
            it.copy(
                //phase = GamePhase.SELECT_LEAGUE,
                phase = GamePhase.SUPERLEAGUE_RANKING,
                currentPair = null,
                selectedIndex = null,
                canProceed = false
            )
        }
     loadGlobalRanking()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadGlobalRanking() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val apiCastles = NetworkModule.api.getAllCastles()
                globalRepository.loadGlobalSuperLeagueRanking(
                    apiCastles = apiCastles,
                    onSuccess = { list ->
                        _uiState.update {
                            it.copy(globalRanking = list, isLoading = false)
                        }
                    },
                    onError = { e ->
                        Log.e("Firestore", "Global ranking load failed", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load global ranking from database."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch API castles for global ranking", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Network timeout or error while fetching castles. Please try again."
                    )
                }
            }
        }
    }



    fun backToMenu() {
        Log.d("GameViewModel", "Back to menu")

        _uiState.update {
            it.copy(
                phase = GamePhase.SELECT_LEAGUE,

                // reseteljük a SuperLeague state-et
                superLeagueWinner = null,
                globalRanking = emptyList(),

                // opcionális: ha új szezon indul
                completedLeagues = emptySet(),
                leagueWinner = null,
                superLeagueCastles= emptyList(),
                remainingGames = 0,
                selectedIndex = null,
                currentLeague = null,
                leagueLocked = false,
                currentPair = null,
            )
        }
    }


    private fun loadUserSuperLeagueRanking(): List<Pair<CastleItem, Int>> {

        val superLeagueCastles = _uiState.value.superLeagueCastles

        return winCounts
            .toList()
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }
                    .thenComparator { a, b ->

                        if (a.second == b.second) {

                            val key = listOf(a.first, b.first).sorted()
                            val winnerId = headToHead[key[0] to key[1]]

                            when (winnerId) {
                                a.first -> -1
                                b.first -> 1
                                else -> 0
                            }

                        } else {
                            0
                        }
                    }
            )
            .mapNotNull { (castleId, wins) ->
                superLeagueCastles
                    .firstOrNull { it.id == castleId }
                    ?.let { castle -> castle to wins }
            }
    }



    fun goToUserSuperLeagueRanking() {
        _uiState.update {
            it.copy(
                userSuperLeagueRanking = loadUserSuperLeagueRanking(),
                phase = GamePhase.USER_SUPERLEAGUE_RANKING
            )
        }
    }


    fun openCastleInfo(castle: CastleItem) {
        lastRankingPhase = _uiState.value.phase  // ← _uiState not _state
        selectedCastleForInfo = castle
        _uiState.update { it.copy(          // ← _uiState not _state
            phase = GamePhase.CASTLE_INFO,
            castleForInfo = castle
        )}
    }

    fun backFromCastleInfo() {
        _uiState.update { it.copy(
            phase = lastRankingPhase ?: GamePhase.SELECT_LEAGUE,
            castleForInfo = null
        )}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun backToGlobalRanking() {
        _uiState.update {
            it.copy(phase = GamePhase.SUPERLEAGUE_RANKING)
        }
        loadGlobalRanking()
    }

//    Loaded once on init so the drawer can mark countries as played
//    also sets allCountriesPlayed after loading.
    private fun loadPlayedCountries() {
        viewModelScope.launch {
            val played    = repository.loadPlayedCountries()
            val available = _uiState.value.availableCountries
            val allPlayed = available.isNotEmpty() && played.containsAll(available)

            _uiState.update {
                it.copy(
                    playedCountries    = played,
                    allCountriesPlayed = allPlayed
                )
            }
        }
    }


    /**
     * Called when the user taps a country in the drawer.
     *
     * Decision tree:
     *   • Country has a saved ranking  → show USER_COUNTRY_RANKING phase
     *   • No saved ranking             → start the country game as before
     */
    fun onCountrySelectedFromDrawer(country: String) {
        viewModelScope.launch {
            val savedRanking = repository.loadUserCountryRanking(country)
            if (savedRanking != null) {
                _uiState.update {
                    it.copy(
                        currentCountry      = country,
                        userCountryRanking  = savedRanking,
                        phase               = GamePhase.USER_COUNTRY_RANKING
                    )
                }
            } else {
                // No history → start game (existing selectCountry logic)
                selectCountry(country)
            }
        }
    }

    /** "Play Again" from UserCountryRankingScreen — clears saved result, starts game */
    fun replayCountry() {
        val country = _uiState.value.currentCountry ?: return
        selectCountry(country)   // existing function — resets & starts game
    }

    /** "Continue" from UserCountryRankingScreen — proceed to next phase */
    fun continueFromUserCountryRanking() {
        _uiState.update { it.copy(phase = GamePhase.SELECT_LEAGUE) }
    }


    // ── (b) New: startUserLeague() ──────────────────────────────────────────────
//    Triggered by "My League" button. Loads user country winners, buckets
//    them into leagues, then starts the first league that has ≥ 2 castles.

    fun startUserLeague() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userLeagueCastles = repository.loadUserLeagueCastles(
                availableCountries = _uiState.value.availableCountries,
                allCastles         = allCastles
            )

            _uiState.update {
                it.copy(
                    userLeagueCastles          = userLeagueCastles,
                    userLeagueCompletedLeagues = emptySet(),
                    userLeagueTopResults       = emptyMap(),
                    userLeagueWinner           = null,
                    isLoading                  = false
                )
            }

            val firstLeague = League.entries.firstOrNull { league ->
                (userLeagueCastles[league]?.size ?: 0) >= 2
            }

            if (firstLeague == null) {
                _uiState.update { it.copy(infoMessage = "Not enough data to build your personal league.") }
                return@launch
            }

            selectUserLeague(firstLeague)
        }
    }


// ── (c) New: selectUserLeague(league) ───────────────────────────────────────
//    Mirrors selectLeague() but draws castles from userLeagueCastles.

    fun selectUserLeague(league: League) {
        winCounts.clear()
        headToHead.clear()

        val castles = _uiState.value.userLeagueCastles[league].orEmpty()

        if (castles.size < 2) {
            // Skip this league — go to next unplayed one
            val next = League.entries
                .filter {
                    it !in _uiState.value.userLeagueCompletedLeagues &&
                            it != league &&
                            (_uiState.value.userLeagueCastles[it]?.size ?: 0) >= 2
                }
                .firstOrNull()

            if (next != null) {
                selectUserLeague(next)
            } else {
                // All playable leagues done — back to menu
                _uiState.update {
                    it.copy(
                        phase        = GamePhase.SELECT_LEAGUE,
                        currentLeague = null,
                        leagueLocked = false
                    )
                }
            }
            return
        }

        shuffledPairs  = generateAllPairs(castles).shuffled().toMutableList()
        totalGames     = shuffledPairs.size
        val firstPair  = if (shuffledPairs.isNotEmpty()) shuffledPairs.removeAt(0) else null

        _uiState.update { state ->
            state.copy(
                currentLeague    = league,
                phase            = GamePhase.USER_LEAGUE_PLAYING,
                currentPair      = firstPair,
                remainingGames   = totalGames,
                selectedIndex    = null,
                leagueLocked     = true,
                userLeagueWinner = null
            )
        }
    }


// ── (d) New: finishUserLeague()  (private) ──────────────────────────────────
//    Mirrors finishLeague(). Determines winner, saves to completedLeagues.

    private fun finishUserLeague() {
        val state   = _uiState.value
        val league  = state.currentLeague ?: return
        val castles = state.userLeagueCastles[league] ?: return

        val winnerId = winCounts.maxByOrNull { it.value }?.key
        val winner   = castles.firstOrNull { it.id == winnerId }

        Log.d("GameViewModel", "User league winner for $league = $winner")

        val uid = userId
        if (uid != null && winner != null) {
            repository.saveUserLeagueRanking(
                userId     = uid,
                leagueId   = league.name,
                winner = winner,
                allResults = getUserLeagueRanking(),
                onSuccess  = { Log.d("GameViewModel", "User league ranking saved for ${league.name}") },
                onError    = { Log.e("GameViewModel", "Failed to save user league ranking for ${league.name}", it) }
            )
        }

        // 🆕 Accumulate winner into userLeagueTopResults so startUserPersonalSuperLeague() can read it
        val updatedTopResults = state.userLeagueTopResults.toMutableMap()
        if (winner != null) updatedTopResults[league] = winner

        _uiState.update {
            it.copy(
                userLeagueWinner           = winner,
                userLeagueTopResults       = updatedTopResults,
                userLeagueCompletedLeagues = it.userLeagueCompletedLeagues + league,
                phase                      = GamePhase.USER_LEAGUE_WINNER,
                currentPair                = null,
                leagueLocked               = false,
                selectedIndex              = null,
                remainingGames             = 0
            )
        }
    }


// ── (e) New: continueFromUserLeagueWinner() ─────────────────────────────────

    fun continueFromUserLeagueWinner() {
        _uiState.update {
            it.copy(
                phase         = GamePhase.USER_LEAGUE_RANKING,
                currentPair   = null,
                selectedIndex = null
            )
        }
    }


// ── (f) New: getUserLeagueRanking() ─────────────────────────────────────────
//    Mirrors getLeagueRanking() but reads from userLeagueCastles.

    fun getUserLeagueRanking(): List<Pair<CastleItem, Int>> {
        val league  = _uiState.value.currentLeague ?: return emptyList()
        val castles = _uiState.value.userLeagueCastles[league] ?: return emptyList()

        return castles
            .map { castle -> castle to (winCounts[castle.id] ?: 0) }
            .sortedWith(
                compareByDescending<Pair<CastleItem, Int>> { it.second }
                    .thenComparator { a, b ->
                        if (a.second == b.second) {
                            val key      = listOf(a.first.id, b.first.id).sorted()
                            val winnerId = headToHead[key[0] to key[1]]
                            when (winnerId) {
                                a.first.id -> -1
                                b.first.id -> 1
                                else       -> 0
                            }
                        } else 0
                    }
            )
    }


// ── (g) New: continueFromUserLeagueRanking() ────────────────────────────────
//    If more playable leagues remain → start next one.
//    If all done → back to menu.

    fun continueFromUserLeagueRanking() {
        winCounts.clear()
        headToHead.clear()

        val state     = _uiState.value
        val completed = state.userLeagueCompletedLeagues
        val remaining = League.entries.filter { league ->
            league !in completed &&
                    (state.userLeagueCastles[league]?.size ?: 0) >= 2
        }

        if (remaining.isEmpty()) {
            // All playable leagues done → personal superleague
            startUserPersonalSuperLeague()
        } else {
            // Pick next league
            selectUserLeague(remaining.first())
        }
    }


    // ── (h) New: startUserPersonalSuperLeague()
//    Takes the winner from each user league and runs a superleague between them.

    fun startUserPersonalSuperLeague() {
        val superCastles = _uiState.value.userLeagueTopResults.values.toList()

        if (superCastles.size < 2) {
            _uiState.update { it.copy(infoMessage = "Not enough league winners for a personal SuperLeague.") }
            return
        }

        winCounts.clear()
        headToHead.clear()

        shuffledPairs = generateAllPairs(superCastles).shuffled().toMutableList()
        totalGames    = shuffledPairs.size

        val firstPair = if (shuffledPairs.isNotEmpty()) shuffledPairs.removeAt(0) else null

        _uiState.update { state ->
            state.copy(
                phase                        = GamePhase.USER_PERSONAL_SUPERLEAGUE_PLAYING,
                userPersonalSuperLeagueCastles = superCastles,
                currentLeague                = null,
                currentPair                  = firstPair,
                remainingGames               = totalGames,
                selectedIndex                = null,
                leagueLocked                 = true
            )
        }
    }

// ── (i) New: finishUserPersonalSuperLeague()  (private)

    private fun finishUserPersonalSuperLeague() {
        val castles = _uiState.value.userPersonalSuperLeagueCastles
        val winner  = castles.maxByOrNull { winCounts[it.id] ?: 0 }

        // ✅ Build GlobalCastle list — mirrors finishSuperLeague()
        val globalRanking = castles.map { castle ->
            GlobalCastle(
                id          = castle.id,
                title       = castle.title,
                imageUrl    = castle.imageUrl,
                wins        = winCounts[castle.id] ?: 0,
                description = castle.description,
                visiting    = castle.visiting,
                wikiUrl     = castle.wikiUrl,
                country     = castle.country,
                location    = castle.location,
                style       = castle.style,
                built       = castle.built,
            )
        }.sortedByDescending { it.wins }

        // ✅ Save to global_superleague_ranking + global_superleague_weekly_ranking
        globalRepository.saveSuperLeagueResultsWithHistory(
            userId  = userId,
            results = globalRanking,
            onError = { e ->
                Log.w("GameViewModel", "Personal superleague saved locally, will sync later", e)
            }
        )

        _uiState.update {
            it.copy(
                userPersonalSuperLeagueWinner = winner,
                phase          = GamePhase.USER_PERSONAL_SUPERLEAGUE_WINNER,
                currentPair    = null,
                selectedIndex  = null,
                remainingGames = 0
            )
        }
    }

/*    private fun finishUserPersonalSuperLeague() {
        val castles = _uiState.value.userPersonalSuperLeagueCastles
        val winner  = castles.maxByOrNull { winCounts[it.id] ?: 0 }

        _uiState.update {
            it.copy(
                userPersonalSuperLeagueWinner = winner,
                phase         = GamePhase.USER_PERSONAL_SUPERLEAGUE_WINNER,
                currentPair   = null,
                selectedIndex = null,
                remainingGames = 0
            )
        }
    }*/

// ── (j) New: continueFromUserPersonalSuperLeagueWinner()

    fun continueFromUserPersonalSuperLeagueWinner() {
        val castles = _uiState.value.userPersonalSuperLeagueCastles

        val ranking = castles
            .map { castle -> castle to (winCounts[castle.id] ?: 0) }
            .sortedByDescending { it.second }

        _uiState.update {
            it.copy(
                phase                           = GamePhase.USER_PERSONAL_SUPERLEAGUE_RANKING,
                userPersonalSuperLeagueRanking  = ranking,
                currentPair                     = null,
                selectedIndex                   = null
            )
        }
    }

// ── (k) New: backToMenuFromPersonalSuperLeague()

    fun backToMenuFromPersonalSuperLeague() {
        winCounts.clear()
        headToHead.clear()
        _uiState.update {
            it.copy(
                phase                           = GamePhase.SELECT_LEAGUE,
                userLeagueTopResults            = emptyMap(),
                userPersonalSuperLeagueCastles  = emptyList(),
                userPersonalSuperLeagueWinner   = null,
                userPersonalSuperLeagueRanking  = emptyList(),
                currentLeague                   = null,
                currentPair                     = null,
                selectedIndex                   = null,
                leagueLocked                    = false,
                remainingGames                  = 0
            )
        }
    }
}
