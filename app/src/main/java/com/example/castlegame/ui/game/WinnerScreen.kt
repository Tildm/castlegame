import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League

@Composable
fun WinnerScreen(
    league: League?,
    winner: CastleItem,
    onContinue: () -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = if (isLandscape) 8.dp else 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center   // 🔑 KULCS
    ) {

        if (!isLandscape) {
            Text(
                text = league?.let { "${it.name} winner" } ?: "🏆 Super League Winner",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        WinnerBigCard(winner = winner)

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))

        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}


@Composable
fun WinnerBigCard(
    winner: CastleItem,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier
                        .fillMaxHeight(0.6f)     // 🔑 MAGASSÁG limit
                        .aspectRatio(16f / 9f)
                } else {
                    Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(16f / 9f)
                }
            ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        AsyncImage(
            model = winner.imageUrl,
            contentDescription = winner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/*
@Composable
fun WinnerScreen(
    league: League,
    winner: CastleItem,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "${league.name} winner",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        WinnerBigCard(winner = winner)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}


@Composable
fun WinnerBigCard(winner: CastleItem) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = Modifier
            .fillMaxWidth(if (isLandscape) 0.7f else 0.9f) // 🔑 LIMIT
            .aspectRatio(16f / 9f)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        AsyncImage(
            model = winner.imageUrl,
            contentDescription = winner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
*/

