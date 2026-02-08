import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle


@Composable
fun GlobalRankingScreen(
    ranking: List<GlobalCastle>,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🌍 International Ranking",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (ranking.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(ranking) { index, castle ->
                    GlobalRankingRow(
                        position = index + 1,
                        castle = castle
                    )
                }
            }
        }

        // Move the Button here, outside of LazyColumn
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Your Super League")
        }
    }
}

@Composable
fun GlobalRankingRow(
    position: Int,
    castle: GlobalCastle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$position.",
            modifier = Modifier.width(32.dp),
            fontWeight = FontWeight.Bold
        )

        AsyncImage(
            model = castle.imageUrl,
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
            Text(castle.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Wins: ${castle.wins}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun UserSuperLeagueRankingScreen(
    ranking: List<Pair<CastleItem, Int>>,  // ✅ Now includes scores
    onBackToMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🏰 Your Super League Ranking",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(ranking) { index, (castle, wins) ->  // ✅ Destructure the pair
                RankingRow(
                    position = index + 1,
                    castle = castle,
                    score = wins  // ✅ Use actual win count
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBackToMenu
        ) {
            Text("Back to menu")
        }
    }
}

