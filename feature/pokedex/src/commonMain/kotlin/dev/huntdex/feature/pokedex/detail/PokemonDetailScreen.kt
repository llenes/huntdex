package dev.huntdex.feature.pokedex.detail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.EvolutionStep
import dev.huntdex.core.domain.model.LocationEncounter
import dev.huntdex.core.domain.model.PokemonAbility
import dev.huntdex.core.domain.model.PokemonDetail
import dev.huntdex.core.domain.model.PokemonStat
import dev.huntdex.core.domain.model.PokemonType
import huntdex.core.ui.generated.resources.Res as CoreUiRes
import huntdex.core.ui.generated.resources.string_back
import huntdex.core.ui.generated.resources.string_loading
import huntdex.core.ui.generated.resources.string_retry
import huntdex.feature.pokedex.generated.resources.Res
import huntdex.feature.pokedex.generated.resources.ability_hidden
import huntdex.feature.pokedex.generated.resources.section_abilities
import huntdex.feature.pokedex.generated.resources.section_evolution
import huntdex.feature.pokedex.generated.resources.section_locations
import huntdex.feature.pokedex.generated.resources.section_stats
import huntdex.feature.pokedex.generated.resources.section_types
import huntdex.feature.pokedex.generated.resources.stat_attack
import huntdex.feature.pokedex.generated.resources.stat_defense
import huntdex.feature.pokedex.generated.resources.stat_hp
import huntdex.feature.pokedex.generated.resources.stat_sp_atk
import huntdex.feature.pokedex.generated.resources.stat_sp_def
import huntdex.feature.pokedex.generated.resources.stat_speed
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

data class PokemonDetailScreen(val pokemonId: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<PokemonDetailScreenModel> { parametersOf(pokemonId) }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            state.detail?.name?.replaceFirstChar { it.uppercase() }
                                ?: stringResource(CoreUiRes.string.string_loading)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { screenModel.onIntent(PokemonDetailIntent.NavigateBack) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(CoreUiRes.string.string_back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    TextButton(onClick = { screenModel.onIntent(PokemonDetailIntent.Retry) }) {
                        Text(stringResource(CoreUiRes.string.string_retry))
                    }
                }

                state.detail != null -> PokemonDetailContent(
                    detail = state.detail!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun PokemonDetailContent(detail: PokemonDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = detail.spriteUrl,
                contentDescription = detail.name,
                modifier = Modifier.size(150.dp)
            )
        }
        Text(
            "#${detail.id.toString().padStart(4, '0')}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            detail.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${detail.generation.removePrefix("generation-").uppercase()} · " +
                    "${detail.height / 10.0}m · ${detail.weight / 10.0}kg",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (detail.flavorText.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(detail.flavorText, style = MaterialTheme.typography.bodyMedium)
        }

        SectionHeader(stringResource(Res.string.section_types))
        TypesRow(detail.types)

        SectionHeader(stringResource(Res.string.section_stats))
        StatsSection(detail.stats)

        SectionHeader(stringResource(Res.string.section_abilities))
        AbilitiesSection(detail.abilities)

        if (detail.evolutionSteps.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.section_evolution))
            EvolutionSection(detail.evolutionSteps)
        }

        if (detail.locationEncounters.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.section_locations))
            LocationsSection(detail.locationEncounters)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Divider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun TypesRow(types: List<PokemonType>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { SuggestionChip(onClick = {}, label = { Text(it.name.replaceFirstChar { c -> c.uppercase() }) }) }
    }
}

@Composable
private fun StatsSection(stats: List<PokemonStat>) {
    val hp = stringResource(Res.string.stat_hp)
    val attack = stringResource(Res.string.stat_attack)
    val defense = stringResource(Res.string.stat_defense)
    val spAtk = stringResource(Res.string.stat_sp_atk)
    val spDef = stringResource(Res.string.stat_sp_def)
    val speed = stringResource(Res.string.stat_speed)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.forEach { stat ->
            val label = when (stat.name) {
                "hp" -> hp
                "attack" -> attack
                "defense" -> defense
                "special-attack" -> spAtk
                "special-defense" -> spDef
                "speed" -> speed
                else -> stat.name.replaceFirstChar { it.uppercase() }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    stat.baseStat.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { stat.baseStat / 255f },
                    modifier = Modifier.weight(1f).height(6.dp)
                )
            }
        }
    }
}

@Composable
private fun AbilitiesSection(abilities: List<PokemonAbility>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        abilities.forEach { ability ->
            Row {
                Text(
                    ability.name.replace('-', ' ').replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (ability.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.ability_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionSection(steps: List<EvolutionStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { step ->
            val label = if (step.minLevel != null) "Lv.${step.minLevel}" else "→"
            Text(
                "${step.fromName.replaceFirstChar { it.uppercase() }} $label ${step.toName.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LocationsSection(encounters: List<LocationEncounter>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        encounters.forEach { enc ->
            Column {
                Text(enc.locationAreaName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    enc.versions.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
