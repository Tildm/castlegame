package com.example.castlegame.ui.ranking

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.castlegame.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.example.castlegame.data.model.League
import com.example.castlegame.data.sharing.findActivity
import com.example.castlegame.data.sharing.shareRankingOnFacebook
import com.example.castlegame.ui.theme.DeutschGothic
//import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import androidx.compose.runtime.ExperimentalComposeApi
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.widget.ShareDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun LeagueRankingScreen(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onContinue: () -> Unit,
    onPlayAgain: () -> Unit,
    onNextLeague: () -> Unit,
    onMyRanking: () -> Unit,
    isUserLeague: Boolean = false,
) {
    val context  = LocalContext.current
    val activity = context.findActivity()
    val scope    = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var isSharing by remember { mutableStateOf(false) }

    // Pre-load top 6 images as software bitmaps (same pattern as SuperLeagueRankingScreen)
    var preloadedBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    LaunchedEffect(ranking) {
        val top6 = ranking.take(6)
        val bitmaps = top6.map { (castle, _) ->
            withContext(Dispatchers.IO) {
                try {
                    val url = castle.imageUrl.firstOrNull()
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
                        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, 200, 200)
                        drawable.draw(canvas)
                        bmp
                    }
                } catch (e: Exception) {
                    Log.e("LeagueRankingScreen", "Bitmap load failed", e)
                    null
                }
            }
        }
        preloadedBitmaps = bitmaps
    }

    // Hidden off-screen template that gets captured for sharing — identical style to
    // FacebookShareTemplate in SuperLeagueRankingScreen
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .width(1080.dp)
            .height(1080.dp)
            .alpha(0f)
            .zIndex(-1f)
            .capturable(captureController)
    ) {
            LeagueFacebookShareTemplate(
                league           = league,
                ranking          = ranking,
                preloadedBitmaps = preloadedBitmaps,
                isUserLeague     = isUserLeague,
            )

    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Ranking list ─────────────────────────────────────────────
            LazyColumn(
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = (if (isUserLeague) "My " else "") +
                                "${league.name} Ranking"
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                        fontFamily    = DeutschGothic,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 18.sp,
                        letterSpacing = 2.sp,
                        color         = Color.Black,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign     = TextAlign.Center
                    )
                }
                val displayRanking = ranking.take(8)
                itemsIndexed(displayRanking) { index, (castle, score) ->
                    RankingRow(
                        position = index + 1,
                        castle   = castle,
                        score    = score,
                        onClick  = { onCastleClick(castle) }
                    )
                }
            }

            // ── Facebook share button ────────────────────────────────────
            Button(
                onClick = {
                    if (activity != null && !isSharing &&
                        preloadedBitmaps.size == ranking.take(6).size
                    ) {
                        isSharing = true
                        scope.launch {
                            try {
                                delay(200)
                                val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                                shareRankingOnFacebook(activity, bitmap)
                            } catch (e: Exception) {
                                Log.e("LeagueRankingScreen", "Share failed", e)
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                shape   = RoundedCornerShape(24.dp),
                enabled = !isSharing && preloadedBitmaps.size == ranking.take(6).size
            ) {
                if (isSharing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Share my Success on Facebook", color = Color.White)
                }
            }

            // ── Play Again | Next League ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Button(
                    onClick  = onPlayAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 24.dp, bottomStart = 24.dp,
                        topEnd = 0.dp,    bottomEnd = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))
                ) {
                    Text("Play Again", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = onNextLeague,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 0.dp, bottomStart = 0.dp,
                        topEnd = 24.dp,  bottomEnd = 24.dp
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))
                ) {
                    Text("Next League", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── My Ranking | Top Rated tabs ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (isUserLeague) {
                    Button(
                        onClick  = { },
                        enabled  = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 0.dp,    bottomEnd = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF6A5ACD).copy(alpha = 0.4f),
                            disabledContentColor   = Color.White.copy(alpha = 0.6f)
                        )
                    ) { Text("My Ranking ✓", fontSize = 15.sp) }
                } else {
                    OutlinedButton(
                        onClick  = onMyRanking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 0.dp,    bottomEnd = 0.dp
                        )
                    ) {
                        Text("My Ranking", fontFamily = DeutschGothic, fontSize = 15.sp, color = Color(0xFF6A5ACD))
                    }
                }

                if (!isUserLeague) {
                    Button(
                        onClick  = { },
                        enabled  = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 24.dp,  bottomEnd = 24.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF6A5ACD).copy(alpha = 0.4f),
                            disabledContentColor   = Color.White.copy(alpha = 0.6f)
                        )
                    ) { Text("Top Rated ✓", fontSize = 15.sp) }
                } else {
                    OutlinedButton(
                        onClick  = onMyRanking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 24.dp,  bottomEnd = 24.dp
                        )
                    ) {
                        Text("Top Rated", fontFamily = DeutschGothic, fontSize = 15.sp, color = Color(0xFF6A5ACD))
                    }
                }
            }
        }


    }

}

// ── Share template ───────────────────────────────────────────────────────────
// Same visual style as FacebookShareTemplate in SuperLeagueRankingScreen,
// adapted for List<Pair<CastleItem, Int>> (no GlobalCastle needed).

@Composable
private fun LeagueFacebookShareTemplate(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    preloadedBitmaps: List<Bitmap?> = emptyList(),
    isUserLeague: Boolean = false,
) {
    val title = (if (isUserLeague) "My " else "") +
            "${league.name} Top 6"
                .lowercase()
                .replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .width(400.dp)
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text       = title.uppercase(),
            fontSize   = 24.sp,
            fontWeight = FontWeight.Black,
            color      = Color(0xFF1478F6),
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Top 6 rows
        ranking.take(6).forEachIndexed { index, (castle, wins) ->
            val bitmap = preloadedBitmaps.getOrNull(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position
                Text(
                    text     = "#${index + 1}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(35.dp),
                    color    = Color(0xFF6A5ACD),
                )

                // Castle image
                Card(
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap           = bitmap.asImageBitmap(),
                            contentDescription = castle.title,
                            contentScale     = ContentScale.Crop,
                            modifier         = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6A5ACD)))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name + flag + votes
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = castle.title,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text     = getCountryFlag(castle.country),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text     = "$wins ${if (wins == 1) "vote" else "votes"}",
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play Store link baked into the image
        Text(
            text      = "\uD83D\uDD17 play.google.com/store/apps/details?id=com.example.castlegame",
            fontSize  = 10.sp,
            color     = Color(0xFF6A5ACD),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        // Footer CTA
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VOTE NOW IN THE APP!", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.play_store_round_color_icon),
                    contentDescription = "Google Play Store Logo",
                    modifier = Modifier.size(24.dp)
                )


            }



        }


    }




}



/*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import com.example.castlegame.data.sharing.shareRankingOnFacebook
import com.example.castlegame.ui.theme.DeutschGothic
import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun LeagueRankingScreen(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onContinue: () -> Unit,
    onPlayAgain: () -> Unit,
    onNextLeague: () -> Unit,
    onMyRanking: () -> Unit,
    isUserLeague: Boolean = false,   // ← ADD THIS
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var isSharing by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── ONLY the list is inside Capturable ──────────────────────
            Capturable(
                controller = captureController,
                modifier = Modifier.weight(1f),
                onCaptured = { _, _ -> }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                // AFTER:
                                text = (if (isUserLeague) "My " else "") +
                                        "${league.name} Ranking"
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                fontFamily = DeutschGothic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        val displayRanking = ranking.take(8)
                        itemsIndexed(displayRanking) { index, (castle, score) ->
                            RankingRow(
                                position = index + 1,
                                castle = castle,
                                score = score,
                                onClick = { onCastleClick(castle) }
                            )
                        }
                    }
                }
            } // ← Capturable ends here

            // ── Share button (NOT in screenshot) ────────────────────────
            Button(
                onClick = {
                    if (activity != null && !isSharing && ranking.isNotEmpty()) {
                        isSharing = true
                        scope.launch {
                            try {
                                yield()
                                delay(100)
                                val capturedImage = captureController.captureAsync().await()
                                val bitmapToShare = capturedImage.asAndroidBitmap()
                                shareRankingOnFacebook(activity, bitmapToShare)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = !isSharing
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Share my Success on Facebook", color = Color.White)
                }
            }

            // ── Play Again | Next League ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 24.dp, bottomStart = 24.dp,
                        topEnd = 0.dp, bottomEnd = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))
                ) {
                    Text("Play Again", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onNextLeague,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 0.dp, bottomStart = 0.dp,
                        topEnd = 24.dp, bottomEnd = 24.dp
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))
                ) {
                    Text("Next League", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── My Ranking | Top Rated tabs ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // My Ranking — active when isUserLeague, navigates away when on Top Rated
                if (isUserLeague) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 0.dp, bottomEnd = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF6A5ACD).copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("My Ranking ✓", fontSize = 15.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onMyRanking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 0.dp, bottomEnd = 0.dp
                        )
                    ) {
                        Text("My Ranking", fontFamily = DeutschGothic, fontSize = 15.sp, color = Color(0xFF6A5ACD))
                    }
                }

                // Top Rated — active when !isUserLeague, navigates away when on My Ranking
                if (!isUserLeague) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 24.dp, bottomEnd = 24.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF6A5ACD).copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Top Rated ✓", fontSize = 15.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onMyRanking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 24.dp, bottomEnd = 24.dp
                        )
                    ) {
                        Text("Top Rated", fontFamily = DeutschGothic, fontSize = 15.sp, color = Color(0xFF6A5ACD))
                    }
                }
            }
    }}
}
*/

/*-------------------------------------------------------------------------------*/
/*@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun LeagueRankingScreen(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var isSharing by remember { mutableStateOf(false) }

    Scaffold(
   *//*     topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${league.name} ranking"
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontFamily = DeutschGothic,
                        letterSpacing = 2.sp
                    )
                }
            )
        }*//*
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── ONLY the list is inside Capturable ──────────────────────
            Capturable(
                controller = captureController,
                modifier = Modifier.weight(1f),
                onCaptured = { _, _ -> }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${league.name} Ranking"
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                fontFamily = DeutschGothic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        val displayRanking = ranking.take(8)
                        itemsIndexed(displayRanking) { index, (castle, score) ->
                            RankingRow(
                                position = index + 1,
                                castle = castle,
                                score = score,
                                onClick = { onCastleClick(castle) }
                            )
                        }
                    }
                }
            } // ← Capturable ends here

            // ── Share button (NOT in screenshot) ────────────────────────
            Button(
                onClick = {
                    if (activity != null && !isSharing) {
                        isSharing = true
                        scope.launch {
                            try {
                                // 1. Kép elkészítése (Compose típus)
                                val capturedImage = captureController.captureAsync().await()

                                // 2. Konvertálás (Android típus)
                                val bitmapToShare = capturedImage.asAndroidBitmap()

                                // 3. Küldés a Facebooknak
                                shareRankingOnFacebook(activity, bitmapToShare)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2)
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = !isSharing
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Share my Success on Facebook 📢", color = Color.White)
                }
            }

            // ── Continue button ──────────────────────────────────────────
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5ACD)
                )
            ) {
                Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}*/

/*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import com.example.castlegame.ui.theme.DeutschGothic

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueRankingScreen(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${league.name} ranking"
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
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
               val displayRanking = ranking.take(8)
                itemsIndexed(displayRanking) { index, (castle, score) ->
                    RankingRow(
                        position = index + 1,
                        castle = castle,
                        score = score,
                        onClick = { onCastleClick(castle) }
                    )
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Continue")
            }
        }
    }
}*/

@Composable
fun RankingRow(
    position: Int,
    castle: CastleItem,
    score: Int,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(80.dp)
            .background(
                Color.LightGray.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Position badge
        Box(
            modifier = Modifier
                .size(32.dp)
                /*.background(
                    Color.Gray.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center*/
        ) {
            Text(
                text = position.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        AsyncImage(
            model = castle.imageUrl.firstOrNull(),
            contentDescription = castle.title,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = castle.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = castle.country,
                fontSize = 13.sp,
                color = Color.Black
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "$score",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Text(
                text = if (score == 1) "vote" else "votes",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}
/*
@Composable
fun RankingRow(
    position: Int,
    castle: CastleItem,
    score: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(72.dp)
            .background(
                Color.LightGray.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "$position",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        AsyncImage(
            model = castle.imageUrl.firstOrNull(),
            contentDescription = castle.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = castle.title + castle.country,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Text(
            text = "$score vote",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
*/
