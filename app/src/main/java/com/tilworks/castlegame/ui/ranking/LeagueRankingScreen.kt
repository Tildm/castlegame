package com.tilworks.castlegame.ui.ranking

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
import com.tilworks.castlegame.R
import com.tilworks.castlegame.data.model.CastleItem
import com.tilworks.castlegame.data.model.League
import com.tilworks.castlegame.data.sharing.findActivity
import com.tilworks.castlegame.data.sharing.shareRankingOnFacebook
import com.tilworks.castlegame.ui.theme.DeutschGothic
//import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp

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

    // Pre-load top 3 images as software bitmaps (same pattern as SuperLeagueRankingScreen)
    var preloadedBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    LaunchedEffect(ranking) {
        val top3 = ranking.take(3)
        val bitmaps = top3.map { (castle, _) ->
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
            .size(width = 1200.dp, height = 530.dp)
            .alpha(0f)
            .zIndex(-1f)
            .capturable(captureController)
           /* .fillMaxWidth()
            .width(1080.dp)
            .height(1080.dp)
            .alpha(0f)
            .zIndex(-1f)
            .capturable(captureController)*/
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
                val displayRanking = if(isUserLeague) ranking.take(6) else ranking.take(3)
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
                        preloadedBitmaps.size == ranking.take(3).size
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
                enabled = !isSharing && preloadedBitmaps.size == ranking.take(3).size
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

private fun League.regionLabel(): String = when (this) {
    League.EAST  -> "Eastern"
    League.WEST  -> "Western"
    League.NORTH -> "Northern"
    League.SOUTH -> "Southern"
}

//itt kezdodik

@Composable
private fun LeagueFacebookShareTemplate(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    preloadedBitmaps: List<Bitmap?> = emptyList(),
    isUserLeague: Boolean = false,
) {
    val title =
        (if (isUserLeague) "MY TOP 3 CASTLES OF " else "TOP 3 CASTLES OF ")
    // + "${league.regionLabel()} EUROPE"

    // Designed for a 4:5 Facebook image, e.g. 1080 x 1350.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val width = maxWidth

        // Responsive sizes based on the width of the generated image.
        val horizontalPadding = width * 0.06f
        val headerHeight = 80.dp
        val cardHeight = 100.dp
        val imageSize = width * 0.22f

        /*val horizontalPadding = width * 0.06f
        val headerHeight = width * 0.22f
        val cardHeight = width * 0.17f
        val imageSize = width * 0.22f*/

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ============================================================
            // HEADER
            // ============================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3020A0),
                                Color(0xFF4335D0),
                                Color(0xFF171B80)
                            )
                        )
                    )
            ) {

                // Decorative circles / light effects
                Box(
                    modifier = Modifier
                        .size(width * 0.35f)
                        .offset(x = -width * 0.10f, y = -width * 0.10f)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(width * 0.28f)
                        .align(Alignment.TopEnd)
                        .offset(
                            x = width * 0.08f,
                            y = -width * 0.08f
                        )
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = width * 0.025f
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = title.uppercase(),
                        //text = "TOP 3 CASTLES OF",
                        fontSize = (width.value * 0.045f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(width * 0.008f))

                    Text(
                        text = "${league.regionLabel()} EUROPE".uppercase(),
                        fontSize = (width.value * 0.065f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }


            // ============================================================
            // TOP 3
            // ============================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = width * 0.025f
                    ),
                verticalArrangement = Arrangement.spacedBy(width * 0.018f)
            ) {

                ranking.take(3).forEachIndexed { index, (castle, wins) ->

                    val bitmap = preloadedBitmaps.getOrNull(index)

                    CastleRankingCard(
                        position = index + 1,
                        castle = castle,
                        wins = wins,
                        bitmap = bitmap,
                        width = width,
                        cardHeight = cardHeight,
                        imageSize = imageSize
                    )
                }
            }


            // ============================================================
            // PLAY STORE LINK
            // ============================================================

            Spacer(
                modifier = Modifier.height(width * 0.015f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "🔗",
                    fontSize = (width.value * 0.028f).sp
                )

                Spacer(
                    modifier = Modifier.width(width * 0.012f)
                )

                Text(
                    text = "play.google.com/store/apps/details?id=com.tilworks.castlegame",
                    fontSize = (width.value * 0.026f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3925B8),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            // ============================================================
            // CTA
            // ============================================================

            Spacer(
                modifier = Modifier.height(width * 0.018f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .height(width * 0.12f)
                    .clip(RoundedCornerShape(width * 0.025f))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3622B8),
                                Color(0xFF5944E5)
                            )
                        )
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = width * 0.035f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "VOTE NOW",
                        fontSize = (width.value * 0.040f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )

                    Spacer(
                        modifier = Modifier.width(width * 0.012f)
                    )

                    Text(
                        text = "IN THE APP!",
                        fontSize = (width.value * 0.040f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        maxLines = 1
                    )

                    Spacer(
                        modifier = Modifier.width(width * 0.025f)
                    )

                    Image(
                        painter = painterResource(
                            id = R.drawable.play_store_round_color_icon
                        ),
                        contentDescription = "Google Play Store",
                        modifier = Modifier.size(width * 0.065f)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(width * 0.025f)
            )
        }
    }
}


@Composable
private fun CastleRankingCard(
    position: Int,
    castle: CastleItem,
    wins: Int,
    bitmap: Bitmap?,
    width: Dp,
    cardHeight: Dp,
    imageSize: Dp
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = RoundedCornerShape(width * 0.025f),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = width * 0.008f
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(width * 0.018f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ============================================================
            // RANK
            // ============================================================

            Box(
                modifier = Modifier
                    .width(width * 0.18f)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(width * 0.025f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3020A0),
                                Color(0xFF5644D9)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "#$position",
                    fontSize = (width.value * 0.045f).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }


            Spacer(
                modifier = Modifier.width(width * 0.018f)
            )


            // ============================================================
            // CASTLE IMAGE
            // ============================================================

            Card(
                modifier = Modifier.size(imageSize),
                shape = RoundedCornerShape(width * 0.025f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE9E7FF)
                )
            ) {

                if (bitmap != null) {

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = castle.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4335C8)),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "🏰",
                            fontSize = (width.value * 0.06f).sp
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.width(width * 0.025f)
            )


            // ============================================================
            // NAME + VOTES
            // ============================================================

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = width * 0.012f),
                verticalArrangement = Arrangement.Center
            ) {

                val nameSize = when {
                    castle.title.length > 25 -> 12f
                    castle.title.length > 19 -> 15f
                    else -> 19f
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = castle.title,
                        fontSize = nameSize.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF171717),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(
                        modifier = Modifier.width(width * 0.012f)
                    )

                    Text(
                        text = getCountryFlag(castle.country),
                        fontSize = (width.value * 0.038f).sp,
                        maxLines = 1
                    )
                }


                Spacer(
                    modifier = Modifier.height(width * 0.008f)
                )


                // Votes with heart icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(width * 0.035f)
                            .clip(CircleShape)
                            .background(Color(0xFF4932C8)),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "♥",
                            fontSize = (width.value * 0.020f).sp,
                            color = Color.White
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(width * 0.012f)
                    )

                    Text(
                        text = "$wins ${if (wins == 1) "vote" else "votes"}",
                        fontSize = (width.value * 0.026f).sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

//**************************************************************************************************
/*
@Composable
private fun LeagueFacebookShareTemplate(
    league: League,
    ranking: List<Pair<CastleItem, Int>>,
    preloadedBitmaps: List<Bitmap?> = emptyList(),
    isUserLeague: Boolean = false,
) {
    val title = (if (isUserLeague) "My Top 3 Castles of " else "Top 3 Castles of ") +
            "${league.regionLabel()} Europe"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text       = title.uppercase(),
            fontSize   = 28.sp,
            fontWeight = FontWeight.Black,
            color      = Color(0xFF1478F6),
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Top 3 rows
        ranking.take(3).forEachIndexed { index, (castle, wins) ->
            val bitmap = preloadedBitmaps.getOrNull(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position
                Text(
                    text     = "#${index + 1}",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp),
                    color    = Color(0xFF6A5ACD),
                )

                // Castle image
                Card(
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
                    shape    = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(80.dp)
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

                Spacer(modifier = Modifier.width(28.dp))

                // Name + flag + votes
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = castle.title,
                            fontSize   = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text     = getCountryFlag(castle.country),
                            fontSize = 28.sp
                        )
                    }
                    Text(
                        text     = "$wins ${if (wins == 1) "vote" else "votes"}",
                        fontSize = 17.sp,
                        color    = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Play Store link baked into the image
        Text(
            text      = "\uD83D\uDD17 play.google.com/store/apps/details?id=com.tilworks.castlegame",
            fontSize  = 20.sp,
            color     = Color(0xFF6A5ACD),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Footer CTA
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
            shape  = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VOTE NOW IN THE APP!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.play_store_round_color_icon),
                    contentDescription = "Google Play Store Logo",
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
*/

//*******************************************************************************************************

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
import com.tilworks.castlegame.data.model.CastleItem
import com.tilworks.castlegame.data.model.League
import com.tilworks.castlegame.data.sharing.shareRankingOnFacebook
import com.tilworks.castlegame.ui.theme.DeutschGothic
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
import com.tilworks.castlegame.data.model.CastleItem
import com.tilworks.castlegame.data.model.League
import com.tilworks.castlegame.ui.theme.DeutschGothic

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