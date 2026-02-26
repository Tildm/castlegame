package com.example.castlegame.ui.game

import GlobalRankingScreen
import LeagueRankingScreen
import UserSuperLeagueRankingScreen
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.castlegame.R
import com.example.castlegame.ui.theme.DeutschGothic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import toCastleItem

//var isLandscape: Boolean = true

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun GameScreen(
    onLogout: () -> Unit,
    //viewModel: GameViewModel
    viewModel: GameViewModel = viewModel(),


    ) {
    val state by viewModel.uiState.collectAsState()

    Log.d("GameScreen", "RECOMPOSE: phase=${state.phase}, winner=${state.leagueWinner != null}, league=${state.currentLeague}")

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(240.dp)) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Menu",
                    fontFamily = DeutschGothic,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    },
                    label = {
                        Text(
                            text = "Logout",
                            fontFamily = DeutschGothic,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {

        Scaffold(
            topBar = {
                LeagueTopBar(
                    completedLeagues = state.completedLeagues,
                    onLeagueSelected = viewModel::selectLeague,
                    isLeagueLocked = state.leagueLocked,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                GameBottomBar(
                    remaining = state.remainingGames,
                    total = viewModel.totalGames,
                    visible = state.phase == GamePhase.PLAYING ||
                            state.phase == GamePhase.SUPERLEAGUE_PLAYING
                )
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(padding)
                    .padding(
                        top = if (isLandscape) 0.dp else 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
            ) {

                // 🔓 LOGOUT SOR
                /* Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(horizontal = 16.dp, vertical = 3.dp),
                     horizontalArrangement = Arrangement.End
                 ) {
                     TextButton(onClick = onLogout) {
                         Text(
                             color = Color(0xFF1478F6),
                             text = "Logout"
                         )
                     }
                 }*/

                // 🎮 JÁTÉKTÉR
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(top = if (isLandscape) 10.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // ← Background for ALL screens except SELECT_LEAGUE
                    if (state.phase != GamePhase.SELECT_LEAGUE) {
                        Image(
                            painter =  painterResource(id = R.drawable.backround_for_castle_games_compass
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    when (state.phase) {

                        GamePhase.SELECT_LEAGUE -> {
                            Log.d("GameScreen", "Rendering: SELECT_LEAGUE")

                            // State to track which background to show
                            var showFirstBackground by remember { mutableStateOf(true) }

                            // LaunchedEffect to alternate backgrounds every 3 seconds
                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(10000) // 3 seconds
                                    showFirstBackground = !showFirstBackground
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {

                                if (isLandscape) {
                                    // Landscape mode - single background
                                    Image(
                                        painter = painterResource(id = R.drawable.backround_for_castle_games3_landscape),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Portrait mode - crossfade between two backgrounds
                                    Crossfade(
                                        targetState = showFirstBackground,
                                        animationSpec = tween(durationMillis = 8000), // 1 second fade
                                        label = "background_crossfade"
                                    ) { isFirst ->
                                        Image(
                                            painter = if (isFirst) {
                                                painterResource(id = R.drawable.backround_duel_europe)
                                            } else {
                                                painterResource(id = R.drawable.trumpet_herold_background)
                                            },
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Optional dark overlay so the top bar league names are visible
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.15f))
                                )
                            }
                        }
                        /*       GamePhase.SELECT_LEAGUE -> {                                //without animation
                                   Log.d("GameScreen", "Rendering: SELECT_LEAGUE")

                                   Box(modifier = Modifier.fillMaxSize()) {

                                       // Background image
                                       Image(
                                           painter = if(isLandscape) painterResource(id = R.drawable.backround_for_castle_games3_landscape)  else painterResource(id = R.drawable.backround_duel_europe),
                                           contentDescription = null,
                                           contentScale = ContentScale.Crop,
                                           modifier = Modifier.fillMaxSize()
                                       )

                                       // Optional dark overlay so the top bar league names are visible
                                       Box(
                                           modifier = Modifier
                                               .fillMaxSize()
                                               .background(Color.Black.copy(alpha = 0.15f))
                                       )
                                   }
                               }*/


                        GamePhase.PLAYING -> {

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                // 🏳 Liga cím
                                if(!isLandscape)
                                {    state.currentLeague?.let { league ->
                                    Text(
                                        text = league.name
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() } + " League",
                                        fontFamily = DeutschGothic,
                                        fontSize = 28.sp,
                                        letterSpacing = 2.sp,
                                        color = Color(0xFFFFD700), // arany
                                        //color = Color.White,
                                        modifier = Modifier
                                            .padding(bottom = 25.dp)
                                    )
                                } }

                                state.currentPair?.let { pair ->
                                    CastleRow(
                                        pair = pair,
                                        selectedIndex = state.selectedIndex,
                                        enabled = true,
                                        onSelect = viewModel::onCastleSelected,
                                        currentLeague = state.currentLeague,
                                    )
                                }
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

                            LeagueRankingScreen(
                                league = state.currentLeague!!,
                                ranking = viewModel.getLeagueRanking(state.currentLeague!!),
                                onContinue = viewModel::continueFromRanking,
                                onCastleClick = { castle ->
                                    viewModel.openCastleInfo(castle)
                                }
                            )
                        }



                        GamePhase.SUPERLEAGUE_PLAYING -> {
                            Log.d("GameScreen", "Rendering: SUPERLEAGUE_PLAYING")

                            Image(
                                painter =  painterResource(id = R.drawable.backround_for_castle_games3
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                if(!isLandscape) {     Text(
                                    text = "Champions League",
                                    fontFamily = DeutschGothic,
                                    fontSize = 28.sp,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFFFFD700), // arany
                                    //color = Color.White,
                                    modifier = Modifier
                                        .padding(bottom = 25.dp)
                                )
                                }
                                state.currentPair?.let { pair ->
                                    CastleRow(
                                        pair = pair,
                                        currentLeague = state.currentLeague,
                                        selectedIndex = state.selectedIndex,
                                        enabled = true,
                                        onSelect = viewModel::onCastleSelected
                                    )
                                } ?: run {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        GamePhase.SUPERLEAGUE_WINNER -> {
                            Log.d("GameScreen", "Rendering: SUPERLEAGUE_WINNER")

                            val winner = state.superLeagueWinner

                            if (winner != null) {
                                WinnerScreen(
                                    league = null, // ⬅️ fontos, lásd lent
                                    winner = state.superLeagueWinner!!,
                                    onContinue = viewModel::continueFromSuperLeagueWinner
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }

                        GamePhase.SUPERLEAGUE_RANKING -> {

                            GlobalRankingScreen(
                                ranking = state.globalRanking,
                                onContinue = viewModel::goToUserSuperLeagueRanking,
                                onCastleClick = { globalCastle ->
                                    viewModel.openCastleInfo(globalCastle.toCastleItem())
                                }
                            )
                        }



                        GamePhase.USER_SUPERLEAGUE_RANKING -> {
                            UserSuperLeagueRankingScreen(
                                ranking = state.userSuperLeagueRanking,
                                onCastleClick = { castle ->
                                    viewModel.openCastleInfo(castle)
                                },
                                onBackToMenu = viewModel::backToMenu,
                                onBackToInternational = viewModel::backToGlobalRanking
                            )
                        }



                        GamePhase.CASTLE_INFO -> {
                            val castle = state.castleForInfo
                            if (castle != null) {
                                WinnerScreen(
                                    league = null,
                                    winner = castle,
                                    onContinue = viewModel::backFromCastleInfo
                                    // onBack = viewModel::backFromCastleInfo
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }

                    }


                }
            }
        }
    } // end ModalNavigationDrawer
}


@Composable
fun LeagueTopBar(
    completedLeagues: Set<League>,
    onLeagueSelected: (League) -> Unit,
    isLeagueLocked: Boolean,
    onMenuClick: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
            .statusBarsPadding()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fleur-de-lis menu button on the left
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fleur_de_lis),
                    contentDescription = "Menu",
                    modifier = Modifier.size(28.dp)
                )
            }

            League.entries.forEach { league ->
                val isNotCompleted = league !in completedLeagues
                val enabled = isNotCompleted && !isLeagueLocked

                Text(
                    text = league.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Normal,
                    fontFamily = DeutschGothic,
                    letterSpacing = 2.sp,
                    fontSize = 22.sp,
                    color = if (enabled) Color(0xFF1478F6) else Color.Gray,
                    modifier = Modifier.clickable(enabled = enabled) {
                        onLeagueSelected(league)
                    }
                )
            }
        }
    }
}



// two cards to choose
@SuppressLint("Range")
@Composable
fun CastleCard(
    castle: CastleItem,
    currentLeague: League?,
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
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            AsyncImage(
                //model = castle.imageUrl,
                model = castle.imageUrl.firstOrNull(),
                contentDescription = "${castle.title} - ${castle.country}",
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
                    text = "${castle.title} - ${castle.country}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}





@Composable
fun CastleRow(
    pair: Pair<CastleItem, CastleItem>,
    currentLeague: League?,   // ← ADD
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
            currentLeague = currentLeague,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(0) },

            )

        CastleCard(
            castle = pair.second,
            currentLeague = currentLeague,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(1) },
        )
    }
}

@Composable
fun GameBottomBar(
    remaining: Int,
    total: Int,
    visible: Boolean
) {
    if (!visible) return

    val progress = if (total > 0) remaining.toFloat() / total.toFloat() else 0f

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,   // ← matches CastleRow's horizontal padding
                end = 16.dp,
                bottom = 56.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),  // ← fills the already-inset Box
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()  // ← Ensure Column fills the Card width
                    .padding(
                        horizontal = 20.dp,
                        vertical = if (isLandscape) 8.dp else 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isLandscape) {
                    Text(
                        text = "$remaining GAMES LEFT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1478F6)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 5.dp else 10.dp)
                        .clip(RoundedCornerShape(50)),
                    trackColor = Color.LightGray,
                    color = Color(0xFF1478F6)
                )
            }
        }
    }
}
