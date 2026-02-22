package com.example.castlegame.ui.game

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import androidx.core.net.toUri

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
}

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

