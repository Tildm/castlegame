import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.castlegame.ui.global.GlobalViewModel

@Composable
fun GlobalLeagueScreen(
    leagueId: String,
    viewModel: GlobalViewModel = viewModel()
) {
    val ranking by viewModel.ranking.collectAsState()

    LaunchedEffect(leagueId) {
        viewModel.loadLeague(leagueId)
    }

    LazyColumn {
        items(ranking) { castle ->
            Text("${castle.title} – ${castle.wins}")
        }
    }
}
