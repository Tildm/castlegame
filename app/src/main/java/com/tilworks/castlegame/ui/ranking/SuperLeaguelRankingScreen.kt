package com.tilworks.castlegame.ui.ranking

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.imageLoader
import coil.request.ImageRequest
import com.tilworks.castlegame.R
import com.tilworks.castlegame.data.model.CastleItem
import com.tilworks.castlegame.data.model.GlobalCastle
import com.tilworks.castlegame.data.sharing.findActivity
import com.tilworks.castlegame.data.sharing.shareRankingOnFacebook
import com.tilworks.castlegame.ui.theme.DeutschGothic
import com.tilworks.castlegame.ui.tooltip.HeraldTooltipDialog
//import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toCastleItem
import com.tilworks.castlegame.ui.tooltip.TooltipRepository
import com.tilworks.castlegame.ui.tooltip.TooltipTranslation


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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    allCountriesPlayed: Boolean = false,
    mySuperLeaguePlayed: Boolean = false,
    onMyEuroLeagueClick: () -> Unit = {},
    onMySuperLeagueClick: () -> Unit = {},
    onPlaySuperLeague: () -> Unit = {},
)
{

    // ── Tooltip state ─────────────────────────────────────────────────────────
    var showTooltip by remember { mutableStateOf(false) }

    val tooltips by produceState(initialValue = TooltipTranslation()) {
        value = TooltipRepository.getSuperLeagueTooltips()
    }

    if (showTooltip && tooltips.myEuroLeagueTooltip.isNotBlank()) {
        HeraldTooltipDialog(
            tooltipText = tooltips.myEuroLeagueTooltip,
            onDismiss   = { showTooltip = false }
        )
    }

    // ── Facebook sharing state ──────────────────────────────────────────────
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var isSharing by remember { mutableStateOf(false) }

    // Pre-load top 3 images as software bitmaps
    var preloadedBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    LaunchedEffect(ranking) {
        if (ranking.isEmpty()) return@LaunchedEffect
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
                    Log.e("UserSuperLeagueRankingScreen", "Bitmap load failed", e)
                    null
                }
            }
        }
        preloadedBitmaps = bitmaps
    }

    // Hidden off-screen template captured for sharing
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
        UserSuperLeagueFacebookShareTemplate(
            ranking          = ranking,
            isPersonalSuperLeague = isPersonalSuperLeague,
            preloadedBitmaps = preloadedBitmaps,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    if (isPersonalSuperLeague) "My Super League Ranking"
                    else "Your Super League Ranking",
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

            // ── Facebook share button ────────────────────────────────────
            Button(
                onClick = {
                    if (activity != null && !isSharing &&
                        ranking.isNotEmpty() &&
                        preloadedBitmaps.size == ranking.take(3).size
                    ) {
                        isSharing = true
                        scope.launch {
                            try {
                                delay(200)
                                val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                                shareRankingOnFacebook(activity, bitmap)
                            } catch (e: Exception) {
                                Log.e("UserSuperLeagueRankingScreen", "Share failed", e)
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
                enabled = !isSharing && ranking.isNotEmpty() &&
                        preloadedBitmaps.size == ranking.take(3).size
            ) {
                if (isSharing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Share my Success on Facebook 📢", color = Color.White)
                }
            }

            val nextButtonLabel: String = when {
                isPersonalSuperLeague -> if (superLeaguePlayed) "Begin a New Quest" else "SuperLeague"
                !myEuroLeaguePlayed   -> "MyEuroLeague"
                !mySuperLeaguePlayed  -> "MySuperLeague"
                else                  -> "Begin a New Quest"
            }

            val nextButtonAction: () -> Unit = when {
                isPersonalSuperLeague -> if (superLeaguePlayed) onBackToMenu else onPlaySuperLeague
                !myEuroLeaguePlayed   -> if (allCountriesPlayed) onMyEuroLeagueClick
                else { { showTooltip = true } }
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
                        modifier = Modifier.weight(0.5f),
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
private fun UserSuperLeagueFacebookShareTemplate(
    ranking: List<Pair<CastleItem, Int>>,
    isPersonalSuperLeague: Boolean = false,
    preloadedBitmaps: List<Bitmap?> = emptyList(),
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val width = maxWidth

        val horizontalPadding = width * 0.06f
        val headerHeight = 80.dp
        val cardHeight = 100.dp
        val imageSize = width * 0.22f

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

                // Decorative circle - left
                Box(
                    modifier = Modifier
                        .size(width * 0.35f)
                        .offset(
                            x = -width * 0.10f,
                            y = -width * 0.10f
                        )
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                // Decorative circle - right
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
                        text = "MY TOP 3 CASTLES OF",
                        fontSize = (width.value * 0.045f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(
                        modifier = Modifier.height(width * 0.008f)
                    )

                    Text(
                      /*  text = if (isPersonalSuperLeague) {
                            "SUPER LEAGUE"
                        } else {
                            "EUROPE"
                        },*/
                        text = "EUROPE",
                        fontSize = (width.value * 0.065f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                verticalArrangement = Arrangement.spacedBy(
                    width * 0.018f
                )
            ) {

                ranking.take(3).forEachIndexed { index, (castle, score) ->

                    val bitmap = preloadedBitmaps.getOrNull(index)

                    UserSuperLeagueCastleRankingCard(
                        position = index + 1,
                        castle = castle,
                        score = score,
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
                    .clip(
                        RoundedCornerShape(width * 0.025f)
                    )
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
private fun UserSuperLeagueCastleRankingCard(
    position: Int,
    castle: CastleItem,
    score: Int,
    bitmap: Bitmap?,
    width: Dp,
    cardHeight: Dp,
    imageSize: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(
                RoundedCornerShape(width * 0.025f)
            )
            .background(Color(0xFFF5F5F5))
            .padding(
                horizontal = width * 0.025f,
                vertical = width * 0.015f
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ============================================================
        // POSITION
        // ============================================================

        Box(
            modifier = Modifier.width(width * 0.10f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$position",
                fontSize = (width.value * 0.045f).sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3925B8),
                textAlign = TextAlign.Center
            )
        }


        Spacer(
            modifier = Modifier.width(width * 0.015f)
        )


        // ============================================================
        // CASTLE IMAGE
        // ============================================================

        Card(
            modifier = Modifier.size(imageSize),
            shape = RoundedCornerShape(width * 0.02f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6A5ACD)
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
                        .background(Color(0xFF6A5ACD))
                )
            }
        }


        Spacer(
            modifier = Modifier.width(width * 0.025f)
        )


        // ============================================================
        // CASTLE NAME + SCORE
        // ============================================================

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = castle.title,
                    fontSize = (width.value * 0.040f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(
                    modifier = Modifier.width(width * 0.01f)
                )

                Text(
                    text = getCountryFlag(castle.country),
                    fontSize = (width.value * 0.038f).sp
                )
            }

            Spacer(
                modifier = Modifier.height(width * 0.008f)
            )

            Text(
                text = "$score ${if (score == 1) "vote" else "votes"}",
                fontSize = (width.value * 0.030f).sp,
                color = Color.Gray
            )
        }
    }
}
/*
@Composable
private fun UserSuperLeagueFacebookShareTemplate(
    ranking: List<Pair<CastleItem, Int>>,
    isPersonalSuperLeague: Boolean = false,
    preloadedBitmaps: List<Bitmap?> = emptyList(),
) {
    Column(
        modifier = Modifier
            .width(400.dp)
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = if (isPersonalSuperLeague) "MY SUPER LEAGUE TOP 3" else "MY EURO LEAGUE TOP 3",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Black,
            color      = Color(0xFF1478F6),
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        ranking.take(6).forEachIndexed { index, (castle, score) ->
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
                    text       = "#${index + 1}",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.width(35.dp),
                    color      = Color(0xFF6A5ACD),
                )

                Card(
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD)),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap             = bitmap.asImageBitmap(),
                            contentDescription = castle.title,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6A5ACD)))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                        text     = "$score ${if (score == 1) "vote" else "votes"}",
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text      = "\uD83D\uDD17 play.google.com/store/apps/details?id=com.tilworks.castlegame",
            fontSize  = 10.sp,
            color     = Color(0xFF6A5ACD),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

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
*/

/*
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
        ranking.take(3).forEachIndexed { index, globalCastle ->
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

        // Play Store link baked into the image
        Text(
            text      = "\uD83D\uDD17 play.google.com/store/apps/details?id=com.tilworks.castlegame",
            fontSize  = 10.sp,
            color     = Color(0xFF6A5ACD),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

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
                Image(
                    painter = painterResource(id = R.drawable.play_store_round_color_icon),
                    contentDescription = "Google Play Store Logo",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class,
    ExperimentalComposeUiApi::class
)
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
        val top3 = ranking.take(3)
        val bitmaps = top3.map { globalCastle ->
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
            .capturable(captureController)
    ) {

        FacebookShareTemplate(
            ranking = ranking,
            preloadedBitmaps = preloadedBitmaps // Pass them here
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Super League Ranking",
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
                    if (activity != null && !isSharing && preloadedBitmaps.size == ranking.take(3).size) {
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
                enabled = preloadedBitmaps.size == ranking.take(3).size // disable until ready
            )
            {
                if (isSharing) CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                else Text("Share Europe's Ranking")
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
}*/

@Composable
fun FacebookShareTemplate(
    ranking: List<GlobalCastle>,
    preloadedBitmaps: List<Bitmap?> = emptyList()
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val width = maxWidth

        val horizontalPadding = width * 0.06f
        val headerHeight = 80.dp
        val cardHeight = 100.dp
        val imageSize = width * 0.22f

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

                // Decorative circle - left
                Box(
                    modifier = Modifier
                        .size(width * 0.35f)
                        .offset(
                            x = -width * 0.10f,
                            y = -width * 0.10f
                        )
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                // Decorative circle - right
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
                        text = "TOP 3 CASTLES OF",
                        fontSize = (width.value * 0.045f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(
                        modifier = Modifier.height(width * 0.008f)
                    )

                    Text(
                        text = "EUROPE",
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
                verticalArrangement = Arrangement.spacedBy(
                    width * 0.018f
                )
            ) {

                ranking.take(3).forEachIndexed { index, globalCastle ->

                    val castleItem = globalCastle.toCastleItem()
                    val bitmap = preloadedBitmaps.getOrNull(index)

                    FacebookShareRankingCard(
                        position = index + 1,
                        castleItem = castleItem,
                        country = globalCastle.country,
                        score = globalCastle.wins.toInt(),
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
                    .clip(
                        RoundedCornerShape(width * 0.025f)
                    )
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


/**
 * Facebook megosztási kép rangsorkártyája.
 *
 * Ez csak a FacebookShareTemplate vizuális felépítéséhez tartozik.
 * A SuperLeagueRankingScreen működését nem módosítja.
 */
@Composable
private fun FacebookShareRankingCard(
    position: Int,
    castleItem: CastleItem,
    country: String,
    score: Int,
    bitmap: Bitmap?,
    width: Dp,
    cardHeight: Dp,
    imageSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(
                RoundedCornerShape(width * 0.025f)
            )
            .background(Color(0xFFF5F5F5))
            .padding(
                horizontal = width * 0.025f,
                vertical = width * 0.015f
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ============================================================
        // POSITION
        // ============================================================

        Box(
            modifier = Modifier.width(width * 0.10f),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "#$position",
                fontSize = (width.value * 0.045f).sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3925B8),
                textAlign = TextAlign.Center
            )
        }


        Spacer(
            modifier = Modifier.width(width * 0.015f)
        )


        // ============================================================
        // CASTLE IMAGE
        // ============================================================

        Card(
            modifier = Modifier.size(imageSize),
            shape = RoundedCornerShape(width * 0.02f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6A5ACD)
            )
        ) {

            if (bitmap != null) {

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = castleItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF6A5ACD))
                )
            }
        }


        Spacer(
            modifier = Modifier.width(width * 0.025f)
        )


        // ============================================================
        // CASTLE NAME + COUNTRY
        // ============================================================

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = castleItem.title,
                    fontSize = (width.value * 0.040f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(
                    modifier = Modifier.width(width * 0.01f)
                )

                Text(
                    text = getCountryFlag(country),
                    fontSize = (width.value * 0.035f).sp
                )
            }

            Spacer(
                modifier = Modifier.height(width * 0.008f)
            )

            Text(
                text = "$score ${if (score == 1) "vote" else "votes"}",
                fontSize = (width.value * 0.030f).sp,
                color = Color.Gray
            )
        }
    }
}


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeApi::class,
    ExperimentalComposeUiApi::class
)
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
    var preloadedBitmaps by remember {
        mutableStateOf<List<Bitmap?>>(emptyList())
    }

    LaunchedEffect(ranking) {
        val top3 = ranking.take(3)

        val bitmaps = top3.map { globalCastle ->

            withContext(Dispatchers.IO) {

                try {

                    val url = globalCastle
                        .toCastleItem()
                        .imageUrl
                        .firstOrNull()

                    if (url.isNullOrBlank()) {
                        return@withContext null
                    }

                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(200, 200)
                        .build()

                    val result = context.imageLoader.execute(request)

                    if (result !is coil.request.SuccessResult) {
                        return@withContext null
                    }

                    val drawable = result.drawable

                    if (drawable is BitmapDrawable) {

                        drawable.bitmap

                    } else {

                        // Universal fallback:
                        // draw any drawable to bitmap
                        val bmp = Bitmap.createBitmap(
                            200,
                            200,
                            Bitmap.Config.ARGB_8888
                        )

                        val canvas = android.graphics.Canvas(bmp)

                        drawable.setBounds(
                            0,
                            0,
                            200,
                            200
                        )

                        drawable.draw(canvas)

                        bmp
                    }

                } catch (e: Exception) {

                    Log.e(
                        "SuperLeagueRankingScreen",
                        "Bitmap load failed",
                        e
                    )

                    null
                }
            }
        }

        preloadedBitmaps = bitmaps

        Log.d(
            "SuperLeagueRankingScreen",
            "Final bitmaps: ${bitmaps.map { it != null }}"
        )
    }


    // ================================================================
    // FACEBOOK SHARE CAPTURE
    // ================================================================

    // Ez a láthatatlan réteg, amit lefotózunk
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(width = 1200.dp, height = 530.dp)
            .alpha(0f)
            .zIndex(-1f)
            .capturable(captureController)
          /*  .fillMaxWidth()
            .width(1080.dp)
            .height(1080.dp)
            .alpha(0f)
            .zIndex(-1f)
            .capturable(captureController)*/
    ) {

        FacebookShareTemplate(
            ranking = ranking,
            preloadedBitmaps = preloadedBitmaps
        )
    }


    // ================================================================
    // MAIN SCREEN
    // ================================================================

    Scaffold(
        topBar = {

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Super League Ranking",
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

                    if (
                        activity != null &&
                        !isSharing &&
                        preloadedBitmaps.size == ranking.take(3).size
                    ) {

                        isSharing = true

                        scope.launch {

                            delay(200)

                            val bitmap = captureController
                                .captureAsync()
                                .await()
                                .asAndroidBitmap()

                            shareRankingOnFacebook(
                                activity,
                                bitmap
                            )

                            isSharing = false
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2)
                ),

                enabled =
                    preloadedBitmaps.size == ranking.take(3).size
            ) {

                if (isSharing) {

                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                } else {

                    Text("Share Europe's Ranking")
                }
            }
        }

    ) { padding ->

        // A te eredeti LazyColumn-od itt marad változatlanul...
        Column(
            modifier = Modifier.padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                val displayRanking = ranking.take(20)

                itemsIndexed(displayRanking) { index, castle ->

                    // Convert GlobalCastle → CastleItem
                    // to reuse RankingRow
                    RankingRow(
                        position = index + 1,
                        castle = castle.toCastleItem(),
                        score = castle.wins.toInt(),
                        onClick = {
                            onCastleClick(castle)
                        }
                    )
                }
            }


            // Az eredeti "Continue" gombod
            Button(
                onClick = onContinue,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5ACD)
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