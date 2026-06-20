package dev.huntdex.feature.moves.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.domain.model.MoveEntry

data object MoveListScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<MoveListScreenModel>()
        val state by model.state.collectAsState()
        MoveListContent(state = state, onIntent = model::onIntent)
    }
}

@Composable
private fun MoveListContent(state: MoveListState, onIntent: (MoveListIntent) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onIntent(MoveListIntent.Search(it)) },
            label = { Text("Search moves") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val types = listOf(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison",
            "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"
        )
        val damageClasses = listOf("physical", "special", "status")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(types) { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onIntent(MoveListIntent.FilterByType(if (state.selectedType == type) null else type)) },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(damageClasses) { cls ->
                FilterChip(
                    selected = state.selectedDamageClass == cls,
                    onClick = { onIntent(MoveListIntent.FilterByDamageClass(if (state.selectedDamageClass == cls) null else cls)) },
                    label = { Text(cls.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onIntent(MoveListIntent.Retry) }) { Text("Retry") }
                }
            }
            else -> MoveList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun MoveList(state: MoveListState, onIntent: (MoveListIntent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(state.displayedMoves) { index, move ->
            if (index == state.displayedMoves.size - 1 && state.hasMore) {
                LaunchedEffect(index) {
                    onIntent(MoveListIntent.LoadNextPage)
                }
            }
            MoveRow(move = move, onClick = { onIntent(MoveListIntent.SelectMove(move.id)) })
            HorizontalDivider()
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

@Composable
private fun MoveRow(move: MoveEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(move.name.replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
