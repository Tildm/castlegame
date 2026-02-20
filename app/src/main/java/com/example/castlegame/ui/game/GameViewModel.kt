package com.example.castlegame.ui.game


import GamePhase
import LeagueResult
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.data.model.League
import com.example.castlegame.data.repository.FirestoreRepository
import com.example.castlegame.data.repository.LeagueRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class GameViewModel : ViewModel() {

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



    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val leagues = repository.loadLeagues()
            val startLeague = League.WEST
            val pairs = leagues[startLeague].orEmpty()

            _uiState.update {
                it.copy(
                    leagues = leagues,
                    currentLeague = startLeague
                )
            }

            resetGame(pairs)
        }
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onCastleSelected(index: Int) {
       // Log.d("GameViewModel", "onCastleSelected START, index=$index")

        val currentPhase = _uiState.value.phase

        if (currentPhase != GamePhase.PLAYING &&
            currentPhase != GamePhase.SUPERLEAGUE_PLAYING
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

           // Log.d("GameViewModel", "shuffledPairs in CastleSelected = $shuffledPairs")

            if (shuffledPairs.isEmpty()) {
                Log.d("GameViewModel", "if shuffledPairs empty in CastleSelected")

                when (currentPhase) {
                    GamePhase.PLAYING -> finishLeague()
                    GamePhase.SUPERLEAGUE_PLAYING -> finishSuperLeague()
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
            phase != GamePhase.SUPERLEAGUE_PLAYING
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
        if (uid != null) {
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
        }

        if (uid != null && winner != null) {
            repository.saveLeagueResult(
                userId = uid,
                leagueId = league.name,
                winner = winner,
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
            .sortedByDescending { it.second }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

        firestoreRepository.saveSuperLeagueResults(
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

    private fun buildUserSuperLeagueRanking(): List<Pair<CastleItem, Int>> {
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
    }



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

