package com.example.castlegame.ui.game

import LeagueResult
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.castlegame.data.model.CastleItem
//import com.example.castlegame.data.model.ImageTextPair
import com.example.castlegame.data.model.League
import com.example.castlegame.data.repository.FirestoreRepository
import com.example.castlegame.data.repository.LeagueRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
//import com.example.castlegame.data.source.League
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

    //private val tapCounts = mutableStateMapOf<String, Int>()


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
        if (_uiState.value.leagueLocked) return

        val pairs = generatePairs(
            _uiState.value.leagues[league].orEmpty()
        ).toMutableList()

        _uiState.update {
            it.copy(
                currentLeague = league,
                currentPair = pairs.removeFirstOrNull(),
                remainingGames = pairs.size,
                leagueLocked = true,
                selectedIndex = null,
                canProceed = false,
                phase = GamePhase.PLAYING,

            )
        }

        shuffledPairs = pairs
        tapCounts.clear()
    }



    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onCastleSelected(index: Int) {
        val pair = uiState.value.currentPair ?: return

        val selected = if (index == 0) pair.first else pair.second


        tapCounts[selected.id] =
            (tapCounts[selected.id] ?: 0) + 1

        Log.d("GameViewModel", "shuffledPairs in CastleSelected = $shuffledPairs")

// nextPair()

        if (shuffledPairs.isNotEmpty()) {
            nextPair()
        } else {
            finishLeague()
        }

    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun nextPair() {
        Log.d("GameViewModel", "nextPair() called, shuffled size = ${shuffledPairs.size}")

        val state = _uiState.value

        if (state.phase != GamePhase.PLAYING) {
            Log.d("GameViewModel", "nextPair SKIPPED, phase=${state.phase}")
            return
        }

        
        if (shuffledPairs.isEmpty()) {
            Log.d("GameViewModel", "nextPair() called, shuffled size2 = ${shuffledPairs.size}")

            finishLeague()

            Log.d("GameViewModel", "FinishLeague call")
            return
        }

        val next = shuffledPairs.removeFirst()

        _uiState.update {
            it.copy(
                currentPair = next,
                remainingGames = shuffledPairs.size,
                leagueLocked = true
            )
        }
    }



    private fun resetGame(castleItem: List<CastleItem>) {
        tapCounts.clear()

        val shuffled = generatePairs(castleItem).toMutableList()

        _uiState.update {
            it.copy(
                currentPair = shuffled.removeFirstOrNull(),
                remainingGames = shuffled.size,
                selectedIndex = null,
                canProceed = false,
                //leagueLocked = false,
                buttonText = "Next"
            )
        }

        shuffledPairs = shuffled
    }

    private fun finishLeague() {
        val state = _uiState.value
        val league = _uiState.value.currentLeague ?: return
        val updated = _uiState.value.completedLeagues + league
        //val resetSeason = updated.size == League.entries.size

        Log.d("GameViewModel", "finishLeague CALLED")
        Log.d("GameViewModel", "Current phase BEFORE update: ${state.phase}")  // ← ADD THIS
        Log.d("GameViewModel", "league castles = ${_uiState.value.leagues[league]}")



        val winnerId = tapCounts.maxByOrNull { it.value }?.key
        val winner = state.leagues[league]
            ?.firstOrNull { it.id == winnerId }

        Log.d("GameViewModel", "winner = $winner")
        Log.d("GameViewModel", "winnerId = $winnerId")


        // 3️⃣ eltárolás
        if (winner != null) {
            champions[league] = winner
        }


        val ranking = tapCounts
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val result = LeagueResult(
            league = league,
            scores = tapCounts.toMap(),
            ranking = ranking
        )

        val uid = userId
        if (uid != null) {
            firestoreRepository.saveLeagueResult(
                userId = uid,
                result = result,
                onSuccess = {
                    Log.d("Firestore", "League ${league.name} saved")
                },
                onError = {
                    Log.e("Firestore", "Save failed", it)
                }
            )
        }



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
                //buttonText = "Select League"

        }
        Log.d("GameViewModel", "Phase AFTER update: ${_uiState.value.phase}")  // ← ADD THIS
        Log.d("GameViewModel", "leagueWinner AFTER update: ${_uiState.value.leagueWinner}")  // ← ADD THIS

    }

    fun continueFromWinner() {
        _uiState.update {
            it.copy(
                leagueWinner = null,
                phase = GamePhase.LEAGUE_RANKING
            )
        }
    }

    fun continueFromRanking() {

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


    private fun generatePairs(
        list: List<CastleItem>
    ): List<Pair<CastleItem, CastleItem>> =
        list.flatMapIndexed { index, item ->
            list.drop(index + 1).map { item to it }
        }.shuffled()

    fun clearLeagueWinner() {
        _uiState.update {
            it.copy(leagueWinner = null)
        }
    }

    fun getLeagueRanking(league: League): List<Pair<CastleItem, Int>> {
        val items = uiState.value.leagues[league].orEmpty()

        return items
            .map { castle ->
                castle to (tapCounts[castle.id] ?: 0)
            }
            .sortedByDescending { it.second }
    }



}
