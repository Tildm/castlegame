package com.example.castlegame.ui.game


import LeagueResult
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.data.model.League
import com.example.castlegame.data.remote.NetworkModule
import com.example.castlegame.data.repository.FirestoreRepository
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

    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

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
                    availableCountries = countries   // 🆕
                )
            }

            // ✅ Promote last week's country winners into their leagues (runs once per week)
            promoteCountryWinnersIfNewWeek(countries)

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
       // Log.d("GameViewModel", "onCastleSelected START, index=$index")

        val currentPhase = _uiState.value.phase

        if (currentPhase != GamePhase.PLAYING &&
            currentPhase != GamePhase.SUPERLEAGUE_PLAYING &&
            currentPhase != GamePhase.COUNTRY_PLAYING   // 🆕
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

           // Log.d("GameViewModel", "shuffledPairs in CastleSelected = $shuffledPairs")

            if (shuffledPairs.isEmpty()) {
                Log.d("GameViewModel", "if shuffledPairs empty in CastleSelected")

                when (currentPhase) {
                    GamePhase.PLAYING -> finishLeague()
                    GamePhase.SUPERLEAGUE_PLAYING -> finishSuperLeague()
                    GamePhase.COUNTRY_PLAYING -> finishCountryTournament()  // 🆕
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
            phase != GamePhase.COUNTRY_PLAYING   // 🆕
        ) {
           // Log.d("GameViewModel", "nextPair SKIPPED, phase=$phase")
            return
        }

        
        if (shuffledPairs.isEmpty()) {
            Log.d("GameViewModel", "nextPair() called, shuffled size2 = ${shuffledPairs.size}")

            finishLeague()

           // Log.d("GameViewModel", "FinishLeague call")
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
 /*       if (uid != null) {
            firestoreRepository.saveLeagueResult(
                userId = uid,
                result = result,
                onSuccess = {
                  //  Log.d("Firestore", "League ${league.name} saved")
                },
                onError = {
                    Log.e("Firestore", "Save failed", it)
                }
            )
        }*/

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

    // 🌍 After viewing country winner, go to ranking (mirrors continueFromWinner)
    fun continueFromCountryWinner() {
        _uiState.update {
            it.copy(
                phase = GamePhase.COUNTRY_RANKING,
                currentPair = null,
                selectedIndex = null
            )
        }
    }

/*    // 🌍 After viewing country winner, return to main menu
    fun continueFromCountryWinner() {
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
    }*/

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




    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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
    }



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

/*    fun getLeagueRanking(league: League): List<Pair<CastleItem, Int>> {
        val items = uiState.value.leagues[league].orEmpty()

        return items
            .map { castle ->
                castle to (winCounts[castle.id] ?: 0)
            }
            .sortedByDescending { it.second }
    }*/


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun startSuperLeague() {
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

    /*@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun startSuperLeague() {
        Log.d("GameViewModel", "SuperLeague here starts")



        val top2PerLeague = mutableListOf<CastleItem>()
        League.entries.forEach { league ->
            val top2 = getLeagueRanking(league).take(2).map { it.first }  // ← Extract CastleItem from Pair
            top2PerLeague.addAll(top2)
        }

        winCounts.clear()

        shuffledPairs = generateAllPairs(top2PerLeague).shuffled().toMutableList()
        totalGames = shuffledPairs.size
        Log.d("GameViewModel", "totalGames = $totalGames")
        // Get the first pair directly
        val firstPair = if (shuffledPairs.isNotEmpty()) {
            shuffledPairs.removeAt(0)

        } else null

        nextPair()

        _uiState.update { state ->
            state.copy(
                phase = GamePhase.SUPERLEAGUE_PLAYING,
                superLeagueCastles = top2PerLeague,
                currentLeague = null,
                leagueWinner = null,
                selectedIndex = null,
                currentPair = firstPair,  // ← Set the first pair here!
                remainingGames = totalGames,
            )
        }


    }*/


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

        firestoreRepository.saveSuperLeagueResultsWithHistory(
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
                firestoreRepository.loadGlobalSuperLeagueRanking(
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


    /*@RequiresApi(Build.VERSION_CODES.O)
    fun loadGlobalRanking() {
        firestoreRepository.loadGlobalSuperLeagueRanking(
            onSuccess = { list ->
                _uiState.update {
                    it.copy(globalRanking = list)
                }

            },
            onError = {
                Log.e("Firestore", "Global ranking load failed", it)
            }
        )
    }*/


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


    private fun buildUserSuperLeagueRanking(): List<Pair<CastleItem, Int>> {

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
/*    private fun buildUserSuperLeagueRanking(): List<Pair<CastleItem, Int>> {
        Log.d("GameViewModel", "buildUserSuperLeagueRanking")
        val superLeagueCastles = _uiState.value.superLeagueCastles
        return winCounts
            .toList()
            .sortedByDescending { it.second }
            .mapNotNull { (castleId, wins) ->
                superLeagueCastles.firstOrNull { it.id == castleId }?.let { castle ->
                    castle to wins  // ✅ Return both castle and win count
                }
            }
    }*/



    fun goToUserSuperLeagueRanking() {
        _uiState.update {
            it.copy(
                userSuperLeagueRanking = buildUserSuperLeagueRanking(),
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
}
