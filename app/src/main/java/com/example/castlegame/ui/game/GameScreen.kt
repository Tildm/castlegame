package com.example.castlegame.ui.game

import LeagueRankingScreen
import WinnerScreen
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.util.Log
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun GameScreen(
    onLogout: () -> Unit,
    //viewModel: GameViewModel
    viewModel: GameViewModel = viewModel()

) {
    val state by viewModel.uiState.collectAsState()

    Log.d("GameScreen", "RECOMPOSE: phase=${state.phase}, winner=${state.leagueWinner != null}, league=${state.currentLeague}")


    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE


    Scaffold(
        topBar = {
            LeagueTopBar(
                completedLeagues = state.completedLeagues,
                onLeagueSelected = viewModel::selectLeague,
                isLeagueLocked = state.leagueLocked  // ← Add this line
            )
        },

        bottomBar = {
            GameBottomBar(
                remaining = state.remainingGames,
               /* buttonText = state.buttonText,
                enabled = state.canProceed,
                onNext = viewModel::nextPair*/
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
               // .padding(padding)
                .padding(
                    top = if (isLandscape) 0.dp else 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {

            // 🔓 LOGOUT SOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onLogout) {
                    Text("Logout")
                }
            }

            // 🎮 JÁTÉKTÉR
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {


                                when (state.phase) {

                                    GamePhase.SELECT_LEAGUE -> {
                                        Log.d("GameScreen", "Rendering: SELECT_LEAGUE")  // ← ADD

                                        Text("Select a league")
                                    }

                                    GamePhase.PLAYING -> {
                                        Log.d("GameScreen", "Rendering: PLAYING")  // ← ADD

                                        state.currentPair?.let { pair ->
                                            CastleRow(
                                                pair = pair,
                                                selectedIndex = state.selectedIndex,
                                                enabled = true,
                                                onSelect = viewModel::onCastleSelected
                                            )
                                        }
                                    }

                                    GamePhase.LEAGUE_WINNER -> {
                                        Log.d("GameScreen", "Rendering: LEAGUE_WINNER")  // ← ADD

                                        val league = state.currentLeague
                                        val winner = state.leagueWinner

                                        Log.d("GameScreen", "SHOW WINNER: ${state.leagueWinner}")

                                        if (league != null && winner != null) {
                                            WinnerScreen(
                                                league = league,
                                                winner = winner,
                                                onContinue = viewModel::continueFromWinner
                                            )
                                        } else {
                                            // 💣 ide soha nem kéne jutni, de ne crasheljen
                                            CircularProgressIndicator()
                                        }
                                    }

                                    GamePhase.LEAGUE_RANKING -> {
                                        Log.d("GameScreen", "Rendering: LEAGUE_RANKING")  // ← ADD

                                        LeagueRankingScreen(
                                            league = state.currentLeague!!,
                                            ranking = viewModel.getLeagueRanking(state.currentLeague!!),
                                            onContinue = viewModel::continueFromRanking
                                        )
                                    }

                                }


                            }
                        }
                    }
                }

                /*when {
                    state.leagueWinner != null && state.currentLeague == null -> {
                        WinnerScreen(
                            league = state.completedLeagues.last(),
                            winner = state.leagueWinner!!,
                            onContinue = viewModel::clearLeagueWinner
                        )
                    }

                    state.currentPair != null -> {
                        CastleRow(
                            pair = state.currentPair!!,
                            selectedIndex = state.selectedIndex,
                            enabled = state.remainingGames > 0,
                            onSelect = viewModel::onCastleSelected
                        )
                    }
                }*/


            /*   Box(
                   modifier = Modifier
                       .fillMaxSize(),
                   contentAlignment = Alignment.Center
               ) {
                   state.currentPair?.let { pair ->
                       CastleRow(
                           pair = pair,
                           selectedIndex = state.selectedIndex,
                           enabled = state.remainingGames > 0,
                           onSelect = viewModel::onCastleSelected
                       )
                   }
               }*/

/*
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

}
*/


@Composable
fun LeagueTopBar(
    completedLeagues: Set<League>,
    onLeagueSelected: (League) -> Unit,
    isLeagueLocked: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        //modifier = Modifier.statusBarsPadding()
        modifier = Modifier
            .statusBarsPadding()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            League.entries.forEach { league ->
                // A league is clickable if:
                // 1. It has NOT been completed yet, AND
                // 2. No league is currently in progress (locked)
                val isNotCompleted = league !in completedLeagues
                val enabled = isNotCompleted && !isLeagueLocked


                Text(
                    text = league.name,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.Blue else Color.Gray,
                    modifier = Modifier.clickable(
                        enabled = enabled
                    ) {
                        onLeagueSelected(league)
                    }
                )
            }
        }
    }
}

@SuppressLint("Range")
@Composable
fun CastleCard(
    castle: CastleItem,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.aspectRatio(16f / 9f)   // 🌄 fekvő
                } else {
                    Modifier.height(260.dp)          // 📱 álló → FIX
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            AsyncImage(
                model = castle.imageUrl,
                contentDescription = castle.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 180.dp else 200.dp) // 🔑 FIX MAGASSÁG

            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = castle.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}

/*
@Composable
fun CastleCard(
    castle: CastleItem,
    isDimmed: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,

) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE


    */
/*    Card(
            modifier = modifier
                .then(
                    if (landscape) {
                        Modifier
                            .aspectRatio(16f / 9f)   // 🌄 fekvő
                    } else {
                        Modifier
                            .width(160.dp)           // 📱 álló – régi jó méretek
                            .height(260.dp)
                    }
                )
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled, onClick = onClick),
            elevation = CardDefaults.cardElevation(6.dp)
        )*//*

    Card(
        modifier = Modifier
            .then(
                if (isLandscape) {
                    Modifier.aspectRatio(16f / 9f)   // 🌄
                } else {
                    Modifier.height(260.dp)
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            AsyncImage(
                model = castle.imageUrl,
                contentDescription = castle.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)   // ⬅️ EZ FONTOS
            )



            */
/* AsyncImage(
                 model = castle.imageUrl,
                 contentDescription = castle.title,
                 contentScale = ContentScale.Crop,
                 modifier = Modifier
                     .fillMaxWidth()
                     .then(
                         if (landscape) {
                             Modifier.weight(1f)   // kitölti a kártyát
                         } else {
                             Modifier.height(200.dp)
                         }
                     )
             )*//*


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = castle.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}
*/



@Composable
fun CastleRow(
    pair: Pair<CastleItem, CastleItem>,
    selectedIndex: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp) // 🔑 EZ KELL

    ) {
        CastleCard(
            castle = pair.first,
            //isSelected = selectedIndex == 0,
           // isDimmed = selectedIndex == 1,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(0) }
        )

        CastleCard(
            castle = pair.second,
           // isSelected = selectedIndex == 1,
           // isDimmed = selectedIndex == 0,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(1) }
        )
    }
}

@Composable
fun GameBottomBar(
    remaining: Int
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Games left: $remaining",
                fontSize = 16.sp,
                color = Color.Blue
            )
        }
    }
}


