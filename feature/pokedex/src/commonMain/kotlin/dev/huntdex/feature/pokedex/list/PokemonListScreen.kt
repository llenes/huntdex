package dev.huntdex.feature.pokedex.list

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage

private val GENERATIONS = listOf(
    null to "All",
    1 to "Gen I", 2 to "Gen II", 3 to "Gen III", 4 to "Gen IV",
    5 to "Gen V", 6 to "Gen VI", 7 to "Gen VII", 8 to "Gen VIII", 9 to "Gen IX"
)

data object PokemonListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<PokemonListScreenModel>()
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Pokédex") })
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { screenModel.onIntent(PokemonListIntent.Search(it)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search Pokémon…") },
                    singleLine = true
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GENERATIONS) { (id, label) ->
                        FilterChip(
                            selected = state.selectedGeneration == id,
                            onClick = { screenModel.onIntent(PokemonListIntent.FilterByGeneration(id)) },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = { screenModel.onIntent(PokemonListIntent.Retry) }) {
                            Text("Retry")
                        }
                    }
                    else -> PokemonGrid(state, screenModel)
                }
            }
        }
    }
}

@Composable
private fun PokemonGrid(
    state: PokemonListState,
    screenModel: PokemonListScreenModel
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(state.displayedPokemon) { index, entry ->
            if (index == state.displayedPokemon.lastIndex && !state.isLoadingMore && state.hasMore) {
                LaunchedEffect(index) { screenModel.onIntent(PokemonListIntent.LoadNextPage) }
            }
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable {
                    screenModel.onIntent(PokemonListIntent.SelectPokemon(entry.id))
                }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = entry.spriteUrl,
                        contentDescription = entry.name,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "#${entry.id.toString().padStart(4, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entry.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        if (state.isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
