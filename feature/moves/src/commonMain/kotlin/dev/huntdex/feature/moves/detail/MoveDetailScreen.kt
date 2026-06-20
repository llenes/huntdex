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
import huntdex.core.ui.generated.resources.Res as CoreUiRes
import huntdex.core.ui.generated.resources.string_back
import huntdex.core.ui.generated.resources.string_retry
import huntdex.feature.moves.generated.resources.Res
import huntdex.feature.moves.generated.resources.contest_appeal
import huntdex.feature.moves.generated.resources.contest_jam
import huntdex.feature.moves.generated.resources.move_see_more
import huntdex.feature.moves.generated.resources.section_contest
import huntdex.feature.moves.generated.resources.section_learned_by
import huntdex.feature.moves.generated.resources.section_stats
import huntdex.feature.moves.generated.resources.stat_accuracy
import huntdex.feature.moves.generated.resources.stat_category
import huntdex.feature.moves.generated.resources.stat_power
import huntdex.feature.moves.generated.resources.stat_pp
import huntdex.feature.moves.generated.resources.stat_priority
import huntdex.feature.moves.generated.resources.stat_type
import org.jetbrains.compose.resources.stringResource
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
                Button(onClick = { onIntent(MoveDetailIntent.Retry) }) {
                    Text(stringResource(CoreUiRes.string.string_retry))
                }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiRes.string.string_back)
                        )
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
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.section_stats), style = MaterialTheme.typography.titleMedium)
                        StatRow(stringResource(Res.string.stat_type), detail.type.replaceFirstChar { it.uppercase() })
                        StatRow(stringResource(Res.string.stat_category), detail.damageClass.replaceFirstChar { it.uppercase() })
                        StatRow(stringResource(Res.string.stat_power), detail.power?.toString() ?: "—")
                        StatRow(stringResource(Res.string.stat_accuracy), detail.accuracy?.let { "$it%" } ?: "—")
                        StatRow(stringResource(Res.string.stat_pp), detail.pp.toString())
                        StatRow(stringResource(Res.string.stat_priority), detail.priority.toString())
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

            detail.contestEffect?.let { contestEffect ->
                item { ContestSection(contestEffect = contestEffect) }
            }

            item {
                Text(
                    stringResource(Res.string.section_learned_by),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

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

            if (state.hasMoreLearnedBy && !state.showAllLearnedBy) {
                item {
                    OutlinedButton(
                        onClick = { onIntent(MoveDetailIntent.ExpandLearnedBy) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text(stringResource(Res.string.move_see_more))
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
            Text(stringResource(Res.string.section_contest), style = MaterialTheme.typography.titleMedium)
            StatRow(stringResource(Res.string.stat_type), contestEffect.contestType.replaceFirstChar { it.uppercase() })
            StatRow(stringResource(Res.string.contest_appeal), contestEffect.appeal.toString())
            StatRow(stringResource(Res.string.contest_jam), contestEffect.jam.toString())
            if (contestEffect.effectEntry.isNotBlank()) {
                Text(contestEffect.effectEntry, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
