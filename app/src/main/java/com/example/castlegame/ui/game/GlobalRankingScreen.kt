import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.example.castlegame.ui.theme.DeutschGothic


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalRankingScreen(
    ranking: List<GlobalCastle>,
    onCastleClick: (GlobalCastle) -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🌍 International Ranking",
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
                val displayRanking = ranking.take(3)
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
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSuperLeagueRankingScreen(
    ranking: List<Pair<CastleItem, Int>>,
    onCastleClick: (CastleItem) -> Unit,
    onBackToMenu: () -> Unit,
    onBackToInternational: () -> Unit  // ← New parameter for going back
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    "🏰 Your Super League Ranking",
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
                val displayRanking = ranking.take(3)
                itemsIndexed(displayRanking) { index, (castle, wins) ->
                    RankingRow(
                        position = index + 1,
                        castle = castle,
                        score = wins,
                        onClick = { onCastleClick(castle) }
                    )
                }
            }

            // Two buttons side by side like in screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Back button (left side)
                Button(
                    onClick = onBackToInternational,
                    modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                            topStart = 24.dp,
                    bottomStart = 24.dp,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp
                ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5ACD)  // Purple color
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

                // Back to menu button (right side)
                Button(
                    onClick = onBackToMenu,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = 24.dp,
                        bottomEnd = 24.dp
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5ACD)  // Purple color
                    )
                ) {
                    Text(
                        "Back to menu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,

                    )
                }
            }
        }
    }
}


