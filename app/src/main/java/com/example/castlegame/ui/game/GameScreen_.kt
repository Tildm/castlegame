/*
package com.example.castlegame.ui.game

import GlobalRankingScreen
import com.example.castlegame.ui.ranking.LeagueRankingScreen
import UserSuperLeagueRankingScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.castlegame.R
import com.example.castlegame.ui.theme.DeutschGothic
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.castlegame.ui.ranking.CountryRankingScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import toCastleItem

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun GameScreen(
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: GameViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Log.d("GameScreen", "RECOMPOSE: phase=${state.phase}, winner=${state.leagueWinner != null}, league=${state.currentLeague}")

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ─────────────────────────────────────────────────────────────────────────
    // ModalNavigationDrawer wraps EVERYTHING — drawer content + screen content
    // ─────────────────────────────────────────────────────────────────────────
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

                // ── Profile ──────────────────────────────────────────────────
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontFamily = DeutschGothic,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onProfileClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // ── Logout ───────────────────────────────────────────────────
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

                // ── Country section ──────────────────────────────────────────
                if (state.availableCountries.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "By Country",
                        fontFamily = DeutschGothic,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                    LazyColumn {
                        items(state.availableCountries) { country ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = country,
                                        fontFamily = DeutschGothic,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp,
                                        color = if (state.currentCountry == country)
                                            Color(0xFFFFD700)
                                        else
                                            Color.Unspecified
                                    )
                                },
                                selected = state.currentCountry == country,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.selectCountry(country)
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            } // end ModalDrawerSheet
        } // end drawerContent
    ) {
        // ─────────────────────────────────────────────────────────────────────
        // Main screen content lives here, inside the ModalNavigationDrawer's
        // trailing lambda — this is the "content" slot, not drawerContent
        // ─────────────────────────────────────────────────────────────────────
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
                            state.phase == GamePhase.SUPERLEAGUE_PLAYING ||
                            state.phase == GamePhase.COUNTRY_PLAYING
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
                // 🎮 JÁTÉKTÉR
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isLandscape) 10.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background for ALL screens except SELECT_LEAGUE
                    if (state.phase != GamePhase.SELECT_LEAGUE) {
                        Image(
                            painter = painterResource(id = R.drawable.backround_for_castle_games_compass),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    when (state.phase) {

                        GamePhase.SELECT_LEAGUE -> {
                            Log.d("GameScreen", "Rendering: SELECT_LEAGUE")

                            var showFirstBackground by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(10000)
                                    showFirstBackground = !showFirstBackground
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isLandscape) {
                                    Image(
                                        painter = painterResource(id = R.drawable.backround_for_castle_games3_landscape),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Crossfade(
                                        targetState = showFirstBackground,
                                        animationSpec = tween(durationMillis = 8000),
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

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.15f))
                                )
                            }
                        }
                        */
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
                                                }*//*

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
                        // 🌍 Country tournament playing — mirrors PLAYING exactly
                        GamePhase.COUNTRY_PLAYING -> {
                            Log.d("GameScreen", "Rendering: COUNTRY_PLAYING")

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                if (!isLandscape) {
                                    state.currentCountry?.let { country ->
                                        Text(
                                            text = country,
                                            fontFamily = DeutschGothic,
                                            fontSize = 28.sp,
                                            letterSpacing = 2.sp,
                                            color = Color(0xFFFFD700),
                                            modifier = Modifier.padding(bottom = 25.dp)
                                        )
                                    }
                                }

                                state.currentPair?.let { pair ->
                                    CastleRow(
                                        pair = pair,
                                        selectedIndex = state.selectedIndex,
                                        enabled = true,
                                        onSelect = viewModel::onCastleSelected,
                                        currentLeague = null,
                                    )
                                }
                            }
                        }

                        // 🌍 Country tournament winner — reuses WinnerScreen
                        GamePhase.COUNTRY_WINNER -> {
                            Log.d("GameScreen", "Rendering: COUNTRY_WINNER")
                            val winner = state.countryWinner
                            if (winner != null) {
                                WinnerScreen(
                                    league = null,
                                    winner = winner,
                                    onContinue = viewModel::continueFromCountryWinner
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }

                        // 🌍 Country ranking
                        GamePhase.COUNTRY_RANKING -> {
                            Log.d("GameScreen", "Rendering: COUNTRY_RANKING")
                            CountryRankingScreen(
                                country = state.currentCountry ?: "",
                                ranking = viewModel.getCountryRanking(),
                                onContinue = viewModel::continueFromCountryRanking,
                                onCastleClick = { castle ->
                                    viewModel.openCastleInfo(castle)
                                }
                            )
                        }

                        GamePhase.LEAGUE_WINNER -> {
                            Log.d("GameScreen", "Rendering: LEAGUE_WINNER")
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
                                painter = painterResource(id = R.drawable.backround_for_castle_games3),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (!isLandscape) {
                                    Text(
                                        text = "Champions League",
                                        fontFamily = DeutschGothic,
                                        fontSize = 28.sp,
                                        letterSpacing = 2.sp,
                                        color = Color(0xFFFFD700),
                                        modifier = Modifier.padding(bottom = 25.dp)
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
                                    league = null,
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
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }

                    } // end when(state.phase)
                } // end Box (JÁTÉKTÉR)
            } // end Column
        } // end Scaffold content lambda
    } // end ModalNavigationDrawer
}*/
