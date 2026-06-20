package dev.huntdex.feature.moves.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail
import org.koin.core.parameter.parametersOf

data class MoveDetailScreen(val id: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<MoveDetailScreenModel> { parametersOf(id) }
        val state by model.state.collectAsState()
        MoveDetailContent(state = state, onIntent = model::onIntent)
    }
}

@Composable
private fun MoveDetailContent(state: MoveDetailState, onIntent: (MoveDetailIntent) -> Unit) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onIntent(MoveDetailIntent.Retry) }) { Text("Retry") }
            }
        }
        state.detail != null -> MoveDetailLoaded(state = state, detail = state.detail, onIntent = onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveDetailLoaded(state: MoveDetailState, detail: MoveDetail, onIntent: (MoveDetailIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.name.replaceFirstChar { it.uppercase() }) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(MoveDetailIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats section
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stats", style = MaterialTheme.typography.titleMedium)
                        StatRow("Type", detail.type.replaceFirstChar { it.uppercase() })
                        StatRow("Category", detail.damageClass.replaceFirstChar { it.uppercase() })
                        StatRow("Power", detail.power?.toString() ?: "—")
                        StatRow("Accuracy", detail.accuracy?.let { "$it%" } ?: "—")
                        StatRow("PP", detail.pp.toString())
                        StatRow("Priority", detail.priority.toString())
                        if (detail.effectEntry.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(detail.effectEntry, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (detail.flavorText.isNotBlank()) {
                            Text(
                                detail.flavorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Contest section (only shown if move has contest data)
            detail.contestEffect?.let { contestEffect ->
                item { ContestSection(contestEffect = contestEffect) }
            }

            // Learned by section — header
            item {
                Text(
                    "Learned by Pokémon",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Each Pokémon as its own lazy item to avoid eagerly composing hundreds of rows
            items(state.learnedByVisible, key = { it.id }) { pokemon ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.id}.png",
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            pokemon.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            pokemon.learnMethods.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // "Ver más" button item
            if (state.hasMoreLearnedBy && !state.showAllLearnedBy) {
                item {
                    OutlinedButton(
                        onClick = { onIntent(MoveDetailIntent.ExpandLearnedBy) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text("Ver más")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContestSection(contestEffect: MoveContestEffect) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contest", style = MaterialTheme.typography.titleMedium)
            StatRow("Type", contestEffect.contestType.replaceFirstChar { it.uppercase() })
            StatRow("Appeal", contestEffect.appeal.toString())
            StatRow("Jam", contestEffect.jam.toString())
            if (contestEffect.effectEntry.isNotBlank()) {
                Text(contestEffect.effectEntry, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
