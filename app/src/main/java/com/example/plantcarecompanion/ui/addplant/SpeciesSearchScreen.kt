package com.example.plantcarecompanion.ui.addplant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.domain.LookupError
import com.example.plantcarecompanion.domain.Species
import com.example.plantcarecompanion.domain.SpeciesSource
import com.example.plantcarecompanion.domain.canRetry

/**
 * Stateless: shows whatever [state] says and reports taps upward (state hoisting, Lecture 3).
 * It never calls the network or decides anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesSearchScreen(
    query: String,
    state: SearchUiState,
    suggestions: List<Species>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onSpeciesClick: (Species) -> Unit,
    onManualEntry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find your plant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("Search by plant name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )

            if (state is SearchUiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            when (state) {
                SearchUiState.Idle -> SpeciesList(
                    header = "Common houseplants",
                    species = suggestions,
                    notice = null,
                    onRetry = onRetry,
                    onSpeciesClick = onSpeciesClick,
                    onManualEntry = onManualEntry
                )

                SearchUiState.Loading -> CenteredMessage(title = "Searching…", body = null) {
                    CircularProgressIndicator()
                }

                is SearchUiState.Results -> SpeciesList(
                    header = if (state.notice == null) "Results" else null,
                    species = state.species,
                    notice = state.notice,
                    onRetry = onRetry,
                    onSpeciesClick = onSpeciesClick,
                    onManualEntry = onManualEntry
                )

                is SearchUiState.Empty -> CenteredMessage(
                    title = "No plants found for “${state.query}”",
                    body = "Check the spelling, or add it yourself."
                ) {
                    Button(onClick = onManualEntry) { Text("Add manually") }
                }

                is SearchUiState.Error -> CenteredMessage(
                    title = state.error.userMessage(),
                    body = "Nothing saved on this phone matches your search either."
                ) {
                    if (state.error.canRetry) {
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    TextButton(onClick = onManualEntry) { Text("Add manually") }
                }
            }
        }
    }
}

@Composable
private fun SpeciesList(
    header: String?,
    species: List<Species>,
    notice: LookupError?,
    onRetry: () -> Unit,
    onSpeciesClick: (Species) -> Unit,
    onManualEntry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (notice != null) {
            item(key = "notice") { NoticeBanner(error = notice, onRetry = onRetry) }
        }
        if (header != null) {
            item(key = "header") {
                Text(header, style = MaterialTheme.typography.titleMedium)
            }
        }
        items(species, key = { it.id }) { s ->
            SpeciesRow(species = s, onClick = { onSpeciesClick(s) })
        }
        item(key = "manual") {
            TextButton(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) {
                Text("Can't find it? Add it manually")
            }
        }
    }
}

/** Shown above fallback results: why live search didn't work + what the user can do. */
@Composable
private fun NoticeBanner(error: LookupError, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(error.userMessage(), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Showing saved and built-in plants instead.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (error.canRetry) {
                TextButton(onClick = onRetry) { Text("Retry live search") }
            }
        }
    }
}

@Composable
private fun SpeciesRow(species: Species, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (species.imageUrl != null) {
                AsyncImage(
                    model = species.imageUrl,
                    contentDescription = null,                  // decorative: the name is right beside it
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        species.commonName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(species.commonName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (species.scientificName != null) {
                    Text(
                        species.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
                val care = listOfNotNull(
                    species.wateringIntervalDays?.let { "Water every ~$it days" },
                    species.lightRequirement
                ).joinToString(" · ")
                if (care.isNotEmpty()) {
                    Text(care, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (species.source) {
                SpeciesSource.LIVE -> Unit
                SpeciesSource.CACHED -> SourceTag("Saved")
                SpeciesSource.BUILT_IN -> SourceTag("Built-in")
            }
        }
    }
}

@Composable
private fun SourceTag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Slot-based (Lecture 3): the caller passes whatever should sit under the text — spinner or buttons. */
@Composable
private fun CenteredMessage(
    title: String,
    body: String?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.size(16.dp))
        content()
    }
}
