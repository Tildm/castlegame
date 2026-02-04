package com.example.castlegame.ui.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.data.model.League
import com.example.castlegame.data.repository.GlobalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GlobalViewModel : ViewModel() {

    private val repository = GlobalRepository()

    private val _ranking = MutableStateFlow<List<GlobalCastle>>(emptyList())
    val ranking: StateFlow<List<GlobalCastle>> = _ranking


    fun loadLeague(leagueId: String) {
        repository.loadGlobalRanking(leagueId) { list ->
            _ranking.value = list
        }
    }
}
