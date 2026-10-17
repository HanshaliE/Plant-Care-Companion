package com.example.plantcarecompanion.ui.addplant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.domain.DraftErrors
import com.example.plantcarecompanion.domain.MAX_INTERVAL_DAYS
import com.example.plantcarecompanion.domain.MIN_INTERVAL_DAYS
import com.example.plantcarecompanion.domain.PlantDraft
import com.example.plantcarecompanion.ui.GradientHeroCard

// The "Add plant" form. It holds no state of its own: everything shown comes in as a parameter
// and every change is reported back through a callback (AddPlantRoute owns the state).
// Layout: gradient header (same colours as Reminders / Profile) that stays at the top, then a scrollable
// form split into three cards - Photo, Plant details and Care - and a Save button at the end.
@Composable
fun AddPlantScreen(
    draft: PlantDraft, // current values of the form
    errors: DraftErrors, // which fields failed validation
    careInfo: CareInfoState, // status of fetching the suggested watering/light info
    isManual: Boolean, // true when the user is typing the species themselves
    photoBusy: Boolean, // true while a gallery photo is being copied in
    photoError: String?, // message to show if taking/choosing a photo failed
    cameraAvailable: Boolean, // false on devices with no camera (disables "Take photo")
    onSpeciesNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit,
    onLightChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp) // keeps the form a readable width on tablets
                    .fillMaxSize()
            ) {
                // header stays fixed at the top while the form below scrolls
                AddPlantHeader(
                    subtitle = if (isManual) {
                        "Enter your plant's details below."
                    } else {
                        draft.speciesName.ifBlank { "Loading…" }
                    },
                    onBack = onBack
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding() // lifts the form above the keyboard
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PhotoCard(
                        photoUri = draft.photoUri,
                        busy = photoBusy,
                        error = photoError,
                        cameraAvailable = cameraAvailable,
                        onTakePhoto = onTakePhoto,
                        onPickPhoto = onPickPhoto,
                        onRemovePhoto = onRemovePhoto
                    )

                    SectionCard(title = "Plant details") {
                        // the species name is only editable in manual mode; otherwise it is shown in the header
                        if (isManual) {
                            OutlinedTextField(
                                value = draft.speciesName,
                                onValueChange = onSpeciesNameChange,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Plant species") },
                                placeholder = { Text("e.g. Peace Lily") },
                                isError = errors.species, // red outline when the species is empty
                                supportingText = { if (errors.species) Text("Enter the plant's name") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }

                        OutlinedTextField(
                            value = draft.nickname,
                            onValueChange = onNicknameChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Nickname") },
                            isError = errors.nickname,
                            supportingText = { if (errors.nickname) Text("Give your plant a nickname") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = draft.location,
                            onValueChange = onLocationChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Location (optional)") },
                            placeholder = { Text("e.g. Kitchen window") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    SectionCard(title = "Care") {
                        CareInfoStatus(careInfo)

                        OutlinedTextField(
                            value = draft.wateringIntervalDays,
                            onValueChange = onIntervalChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Water every (days)") },
                            isError = errors.interval,
                            supportingText = {
                                Text(
                                    if (errors.interval) "Enter a number from $MIN_INTERVAL_DAYS to $MAX_INTERVAL_DAYS"
                                    else "You can change this any time"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = draft.lightRequirement,
                            onValueChange = onLightChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Light (optional)") },
                            placeholder = { Text("e.g. Bright indirect light") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }

                    // validation happens in the ViewModel; the form only shows the errors it returns
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "Save plant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // breathing room under the button
                }
            }
        }
    }
}

// Gradient header card (same style as the Reminders / Profile screens) with a back arrow, the title
// and a subtitle that shows the chosen species name (or a hint in manual mode)
@Composable
private fun AddPlantHeader(subtitle: String, onBack: () -> Unit) {
    GradientHeroCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // outer margin around the card
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary // light colour so it shows on the gradient
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🌱 Add plant",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// A rounded card with a small heading in the primary colour (like "Account" / "About" on the Profile screen).
// The caller fills it with whatever fields belong in that section.
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

// Tells the user what happened when we tried to fetch watering/light suggestions
@Composable
private fun CareInfoStatus(state: CareInfoState) {
    when (state) {
        CareInfoState.Idle -> Unit // nothing to say yet

        CareInfoState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Fetching care info…", style = MaterialTheme.typography.bodySmall)
        }

        CareInfoState.Loaded -> Text(
            "Suggested watering and light filled in — adjust them if your plant needs something different.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // expected situations (no key / offline / daily limit) are shown calmly; real failures in the error colour
        is CareInfoState.Unavailable -> Text(
            buildString {
                state.error?.let { append(it.userMessage()).append(' ') }
                append("Please enter watering and light yourself.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (state.error == null || state.error.isSoftNotice) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

// Card with the photo preview and the two buttons for adding one (camera or gallery)
@Composable
private fun PhotoCard(
    photoUri: String?,
    busy: Boolean,
    error: String?,
    cameraAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    SectionCard(title = "Photo") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // keeps the same shape at any screen width
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Photo of your plant",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // placeholder shown until a photo is added
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪴", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "No photo yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            // spinner on top of the preview while a gallery photo is being copied in
            if (busy) CircularProgressIndicator()
        }

        // weight(1f) makes the two buttons equal width on any screen
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = onTakePhoto,
                enabled = cameraAvailable && !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Take photo") }
            FilledTonalButton(
                onClick = onPickPhoto,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Gallery") }
        }

        // only offer removal when there is a photo to remove
        if (photoUri != null) {
            TextButton(onClick = onRemovePhoto) { Text("Remove photo") }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
