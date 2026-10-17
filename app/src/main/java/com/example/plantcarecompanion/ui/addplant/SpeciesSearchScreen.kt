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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.plantcarecompanion.ui.GradientHeroCard

@Composable
fun SpeciesSearchScreen(
    query: String, // text currently typed in the search field
    state: SearchUiState, // what to show: idle suggestions, loading, results, empty or error
    suggestions: List<Species>, // built-in common plants shown before the user searches
    onQueryChange: (String) -> Unit, // called every time the search text changes
    onSearch: () -> Unit, // called when the keyboard's search key is pressed (search right now)
    onRetry: () -> Unit, // called when the user taps Retry after a failed search
    onSpeciesClick: (Species) -> Unit, // called with the species the user picked
    onManualEntry: () -> Unit, // called by the floating "Add it manually" button
    onBack: () -> Unit // called by the back arrow in the header
) {
    Scaffold(
        // Scaffold keeps this button floating at the bottom end, above the list, even while scrolling
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onManualEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Can't find it? Add it manually") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SearchHeader(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onBack = onBack
            )

            // thin progress bar under the header while a live search is running
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
                    onSpeciesClick = onSpeciesClick
                )

                SearchUiState.Loading -> CenteredMessage(emoji = "🔍", title = "Searching…", body = null) {
                    CircularProgressIndicator()
                }

                // notice != null means the live search failed and these are the saved/built-in fallback results
                is SearchUiState.Results -> SpeciesList(
                    header = if (state.notice == null) "Results" else null,
                    species = state.species,
                    notice = state.notice,
                    onRetry = onRetry,
                    onSpeciesClick = onSpeciesClick
                )

                is SearchUiState.Empty -> CenteredMessage(
                    emoji = "🌱",
                    title = "No plants found for “${state.query}”",
                    body = "Check the spelling, or tap the button below to add it yourself."
                ) {}

                is SearchUiState.Error -> CenteredMessage(
                    emoji = "📡",
                    title = state.error.userMessage(),
                    body = "Nothing saved on this phone matches your search either."
                ) {
                    if (state.error.canRetry) {
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
    }
}

// Gradient header card (same style as the Reminders / Profile screens): back arrow, title and the search box
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit
) {
    GradientHeroCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // outer margin around the card
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary // white-ish so it shows on the gradient
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🔍 Find your plant",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Search by name and we'll fill in watering and light for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            // solid surface colour inside the field so the typed text is readable on top of the gradient
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            label = { Text("Search by plant name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                // clear button only appears once something has been typed
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
    }
}

// Scrollable list of species, with an optional notice banner and heading above it
@Composable
private fun SpeciesList(
    header: String?,
    species: List<Species>,
    notice: LookupError?,
    onRetry: () -> Unit,
    onSpeciesClick: (Species) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // large bottom padding so the last card can scroll up above the floating "Add it manually" button
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (notice != null) {
            item(key = "notice") { NoticeBanner(error = notice, onRetry = onRetry) }
        }
        if (header != null) {
            item(key = "header") {
                // section heading in the primary colour, like "Account" / "About" on the Profile screen
                Text(
                    header,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        items(species, key = { it.id }) { s ->
            SpeciesRow(species = s, onClick = { onSpeciesClick(s) })
        }
    }
}

// Shown above fallback results to explain why live search didn't run.
// Expected situations (no key, offline, daily limit) use a calm neutral colour; real failures use the error colour.
@Composable
private fun NoticeBanner(error: LookupError, onRetry: () -> Unit) {
    val soft = error.isSoftNotice
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (soft) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            contentColor = if (soft) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
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
            // only offer Retry when trying again could actually help
            if (error.canRetry) {
                TextButton(onClick = onRetry) { Text("Retry live search") }
            }
        }
    }
}

// One species card: thumbnail (or first letter), names, watering/light summary and a source tag
@Composable
private fun SpeciesRow(species: Species, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (species.imageUrl != null) {
                AsyncImage(
                    model = species.imageUrl,
                    contentDescription = null, // decorative: the plant's name is shown right next to it
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                // no picture available: show the first letter on a coloured square
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        species.commonName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    species.commonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (species.scientificName != null) {
                    Text(
                        species.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // short care summary, e.g. "💧 Every ~7 days · ☀️ Bright indirect light"
                val care = listOfNotNull(
                    species.wateringIntervalDays?.let { "💧 Every ~$it days" },
                    species.lightRequirement?.let { "☀️ $it" }
                ).joinToString(" · ")
                if (care.isNotEmpty()) {
                    Text(
                        care,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // small tag showing where the result came from (live results get no tag)
            when (species.source) {
                SpeciesSource.LIVE -> Unit
                SpeciesSource.CACHED -> SourceTag("Saved")
                SpeciesSource.BUILT_IN -> SourceTag("Built-in")
            }
        }
    }
}

// Small rounded pill, same style as the status chips on the Reminders screen
@Composable
private fun SourceTag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// Full-screen centred message used for loading, empty and error states.
// The caller passes whatever should sit under the text (spinner, Retry button, or nothing).
@Composable
private fun CenteredMessage(
    emoji: String,
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
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.size(16.dp))
        content()
    }
}
