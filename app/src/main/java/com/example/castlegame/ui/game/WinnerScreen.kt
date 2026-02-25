package com.example.castlegame.ui.game

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import androidx.core.net.toUri


/*import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import androidx.core.net.toUri*/

@Composable
fun WinnerScreen(
    league: League?,
    winner: CastleItem,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        HeroSection(
            league = league,
            winner = winner
        )

        Spacer(modifier = Modifier.height(16.dp))

        FloatingInfoCard(
            winner = winner,
            isSuperLeague = league == null
        )


        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeroSection(
    league: League?,
    winner: CastleItem
) {
    // Build the image list: primary imageUrl first, then up to 4 extras, capped at 5 total.
    // Assumes CastleItem may have an `imageUrls: List<String>` field with additional images.
    // If that field doesn't exist yet, only the single imageUrl is shown (carousel hidden).
    val allImages: List<String> = remember(winner) {
        buildList {
           // if (winner.imageUrl.isNotBlank()) add(winner.imageUrl.firstOrNull())
            // Uncomment when CastleItem has imageUrls:
            winner.imageUrl.forEach { if (it.isNotBlank() && size < 5) add(it) }
        }.take(5)
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 80f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // --- Image with swipe support ---
        val imageUrl = allImages.getOrElse(currentIndex) { winner.imageUrl }

        AsyncImage(
            model = imageUrl,
            contentDescription = winner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (allImages.size > 1) {
                        Modifier.pointerInput(allImages.size) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragOffset < -swipeThreshold ->
                                            currentIndex = (currentIndex + 1) % allImages.size
                                        dragOffset > swipeThreshold ->
                                            currentIndex = (currentIndex - 1 + allImages.size) % allImages.size
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f },
                                onHorizontalDrag = { _, delta -> dragOffset += delta }
                            )
                        }
                    } else Modifier
                )
        )

        // --- Gradient overlay ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // --- Arrow buttons (only when multiple images) ---
        if (allImages.size > 1) {
            // Left arrow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> }
                    },
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = {
                        currentIndex = (currentIndex - 1 + allImages.size) % allImages.size
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("‹", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
            }

            // Right arrow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = {
                        currentIndex = (currentIndex + 1) % allImages.size
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("›", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
            }

            // --- Dot indicators ---
      /*      Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                allImages.indices.forEach { i ->
                    val isActive = i == currentIndex
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color.White
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }*/
        }

        // --- Text overlay ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                // Leave room for dots if shown
                .padding(end = if (allImages.size > 1) 48.dp else 0.dp)
        ) {
            Text(
                text = league?.let { "🏆 ${it.name} Winner" }
                    ?: "🏆 Super League Winner",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = winner.title.uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            if (winner.location.isNotBlank() || winner.country.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(winner.location)
                        if (winner.location.isNotBlank() && winner.country.isNotBlank()) {
                            append(" • ")
                        }
                        append(winner.country)
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }
    }
}



/*@Composable
private fun HeroSection(
    league: League?,
    winner: CastleItem
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {

        AsyncImage(
            model = winner.imageUrl,
            contentDescription = winner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {

            Text(
                text = league?.let { "🏆 ${it.name} Winner" }
                    ?: "🏆 Super League Winner",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = winner.title.uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            if (winner.location.isNotBlank() || winner.country.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(winner.location)
                        if (winner.location.isNotBlank() && winner.country.isNotBlank()) {
                            append(" • ")
                        }
                        append(winner.country)
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }
    }
}*/

@Composable
private fun FloatingInfoCard(
    winner: CastleItem,
    isSuperLeague: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .offset(y = (-28).dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            InfoLine("🏰", winner.built)
            InfoLine("🧱", winner.style)
            InfoLine("📍", winner.location)
            InfoLine("🕒", winner.visiting)

            if (winner.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = winner.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ✅ ONLY Super League
            if (isSuperLeague && winner.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))

                AssistChip(
                    onClick = {
                        val uri = "geo:0,0?q=${winner.title} ${winner.location}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    },
                    label = { Text("Open in Google Maps") }
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: String, text: String) {
    if (text.isNotBlank()) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(text)
        }
    }
}

