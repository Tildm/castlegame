package com.example.castlegame.ui.ranking

import android.R.attr.enabled
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.data.sharing.findActivity
import com.example.castlegame.data.sharing.shareRankingOnFacebook
import com.example.castlegame.ui.theme.DeutschGothic
import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toCastleItem


/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperLeagueRankingScreen(
    ranking: List<GlobalCastle>,
    onCastleClick: (GlobalCastle) -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🌍 Super League Ranking",
                    fontFamily = DeutschGothic,
                    letterSpacing = 2.sp,) }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayRanking = ranking.take(20)
                itemsIndexed(displayRanking) { index, castle ->
                    // Convert GlobalCastle → CastleItem to reuse RankingRow
                    RankingRow(
                        position = index + 1,
                        castle = castle.toCastleItem(),
                        score = castle.wins.toInt(),
                        onClick = { onCastleClick(castle) }
                    )
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5ACD)  // Purple color from screenshot
                )
            ) {
                Text(
                    "Your Super League",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
*/


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSuperLeagueRankingScreen(
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onBackToMenu: () -> Unit,
    onBackToInternational: () -> Unit,  // ← New parameter for going back
    showBackButton: Boolean = true,
    superLeaguePlayed: Boolean = true,
    isPersonalSuperLeague: Boolean = false,
    myEuroLeaguePlayed: Boolean = false,
    mySuperLeaguePlayed: Boolean = false,
    onMyEuroLeagueClick: () -> Unit = {},
    onMySuperLeagueClick: () -> Unit = {},
    onPlaySuperLeague: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    if (isPersonalSuperLeague) "🏰 My Super League Ranking"
                    else "🏰 Your Super League Ranking",
                    fontFamily = DeutschGothic,
                    letterSpacing = 2.sp,
                    color = Color(0xFF1478F6)
                ) }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayRanking = ranking.take(10) //hány jelenjen meg
                itemsIndexed(displayRanking) { index, (castle, wins) ->
                    RankingRow(
                        position = index + 1,
                        castle = castle,
                        score = wins,
                        onClick = { onCastleClick(castle) }
                    )
                }
            }

            val nextButtonLabel: String = when {
                isPersonalSuperLeague -> if (superLeaguePlayed) "Back to menu" else "SuperLeague"
                !myEuroLeaguePlayed   -> "MyEuroLeague"
                !mySuperLeaguePlayed  -> "MySuperLeague"
                else                  -> "Back to menu"
            }

            val nextButtonAction: () -> Unit = when {
                isPersonalSuperLeague -> if (superLeaguePlayed) onBackToMenu else onPlaySuperLeague
                !myEuroLeaguePlayed   -> onMyEuroLeagueClick
                !mySuperLeaguePlayed  -> onMySuperLeagueClick
                else                  -> onBackToMenu
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (showBackButton) {
                    Button(
                        onClick = onBackToInternational,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart    = 24.dp,
                            bottomStart = 24.dp,
                            topEnd      = 0.dp,
                            bottomEnd   = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A5ACD)
                        )
                    ) {
                        Text(
                            "<",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Button(
                    onClick = if (superLeaguePlayed) nextButtonAction else onBackToMenu,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart    = if (showBackButton) 0.dp else 24.dp,
                        bottomStart = if (showBackButton) 0.dp else 24.dp,
                        topEnd      = 24.dp,
                        bottomEnd   = 24.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5ACD)
                    )
                ) {
                    Text(
                        if (superLeaguePlayed) nextButtonLabel else "Play Super League",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

fun getCountryFlag(country: String): String {
    return when (country) {
        "Austria" -> "🇦🇹"
        "Belgium" -> "🇧🇪"
        "Bulgaria" -> "🇧🇬"
        "Croatia" -> "🇭🇷"
        "Czech Republic" -> "🇨🇿"
        "Denmark" -> "🇩🇰"
        "England" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿"
        "Finland" -> "🇫🇮"
        "France" -> "🇫🇷"
        "Germany" -> "🇩🇪"
        "Greece" -> "🇬🇷"
        "Hungary" -> "🇭🇺"
        "Ireland" -> "🇮🇪"
        "Italy" -> "🇮🇹"
        "Montenegro" -> "🇲🇪"
        "Netherlands" -> "🇳🇱"
        "Poland" -> "🇵🇱"
        "Portugal" -> "🇵🇹"
        "Romania" -> "🇷🇴"
        "Scotland" -> "🏴󠁧󠁢󠁳󠁣󠁴󠁿"
        "Slovakia" -> "🇸🇰"
        "Spain" -> "🇪🇸"
        "Sweden" -> "🇸🇪"
        "Switzerland" -> "🇨🇭"
        else -> "🌍"
    }
}



@Composable
fun FacebookShareTemplate(
    ranking: List<GlobalCastle>,
    preloadedBitmaps: List<Bitmap?> = emptyList()
) {
    Column(
        modifier = Modifier
            .width(400.dp) // Fix szélesség a jó minőségű képhez
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- FEJLÉC  ---
        Text(
            text = "EUROPE'S TOP 6 Castles",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1478F6),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        Log.d("SuperLeagueRankingScreen", "preloadedBitmaps: ${preloadedBitmaps}")
        // --- A TOP 6 LISTA ---
        ranking.take(6).forEachIndexed { index, globalCastle ->
            val castleItem = globalCastle.toCastleItem()
            val bitmap = preloadedBitmaps.getOrNull(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${index + 1}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(35.dp),
                    color = Color(0xFF6A5ACD),

                    )



                // Spacer(modifier = Modifier.height(20.dp))

                // --- LÁBLÉC (Call to Action) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(50.dp)
                )
                {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = castleItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback color placeholder
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6A5ACD)))
                    }
                }


                Spacer(modifier = Modifier.width(12.dp))
                // NÉV + ORSZÁG ZÁSZLÓ
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = castleItem.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Zászló a felirat UTÁN
                        Text(text = getCountryFlag(globalCastle.country), fontSize = 14.sp)
                    }
                    Text(
                        text = "${globalCastle.wins.toInt()} votes",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(20.dp))

            // --- LÁBLÉC (Call to Action) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VOTE NOW IN THE APP!", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Ide jöhet egy kis Google Play ikon imitáció
                    Box(Modifier.size(24.dp).background(Color.White, CircleShape))
                }
            }
        }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun SuperLeagueRankingScreen(
    ranking: List<GlobalCastle>,
    onCastleClick: (GlobalCastle) -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var isSharing by remember { mutableStateOf(false) }

    // Pre-load the top 3 images as bitmaps
    var preloadedBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    LaunchedEffect(ranking) {
        val top6 = ranking.take(6)
        val bitmaps = top6.map { globalCastle ->
            withContext(Dispatchers.IO) {
                try {
                    val url = globalCastle.toCastleItem().imageUrl.firstOrNull()
                    if (url.isNullOrBlank()) return@withContext null

                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(200, 200)
                        .build()

                    val result = context.imageLoader.execute(request)
                    if (result !is coil.request.SuccessResult) return@withContext null

                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        // Universal fallback: draw any drawable to bitmap
                        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, 200, 200)
                        drawable.draw(canvas)
                        bmp
                    }
                } catch (e: Exception) {
                    Log.e("SuperLeagueRankingScreen", "Bitmap load failed", e)
                    null
                }
            }
        }
        preloadedBitmaps = bitmaps
        Log.d("SuperLeagueRankingScreen", "Final bitmaps: ${bitmaps.map { it != null }}")
    }

    // Ez a trükk: A láthatatlan réteg, amit lefotózunk
    Box(
        modifier = Modifier
            .fillMaxWidth()
           .width(1080.dp)
           .height(1080.dp)
            .alpha(0f)
            .zIndex(-1f)
    ) {
        Capturable(controller = captureController, onCaptured = { _, _ -> }) {
            FacebookShareTemplate(
                ranking = ranking,
                preloadedBitmaps = preloadedBitmaps // Pass them here
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "🌍 Super League Ranking",
                        fontFamily = DeutschGothic,
                        letterSpacing = 2.sp,
                    )
                }
            )
        },
        bottomBar = {
            // Facebook Megosztás Gomb
            Button(
                onClick = {
                    if (activity != null && !isSharing && preloadedBitmaps.size == ranking.take(6).size) {
                        isSharing = true
                        scope.launch {
                            delay(200)
                            val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                            shareRankingOnFacebook(activity, bitmap)
                            isSharing = false
                        }
                    }

                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        enabled = preloadedBitmaps.size == ranking.take(6).size // disable until ready
            )
            {
                if (isSharing) CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                else Text("Share Europe's Ranking 🌍")
            }
        }
    ) { padding ->
        // A te eredeti LazyColumn-od itt marad változatlanul...
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayRanking = ranking.take(20)
                itemsIndexed(displayRanking) { index, castle ->
                    // Convert GlobalCastle → CastleItem to reuse RankingRow
                    RankingRow(
                        position = index + 1,
                        castle = castle.toCastleItem(),
                        score = castle.wins.toInt(),
                        onClick = { onCastleClick(castle) }
                    )
                }
            }
            // Az eredeti "Continue" gombod
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5ACD)  // Purple color from screenshot
                )
            ) {
                Text(
                    "Your Super League",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


