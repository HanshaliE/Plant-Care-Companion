package com.example.plantcarecompanion.ui.addplant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.domain.DraftErrors
import com.example.plantcarecompanion.domain.MAX_INTERVAL_DAYS
import com.example.plantcarecompanion.domain.MIN_INTERVAL_DAYS
import com.example.plantcarecompanion.domain.PlantDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    draft: PlantDraft, //contains current form values
    errors: DraftErrors, // errors contains validation errors for the form
    careInfo: CareInfoState, // status of the suggested plant care info
    isManual: Boolean,
    photoBusy: Boolean,
    photoError: String?,
    cameraAvailable: Boolean,
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
    Scaffold( //provides the main structure of the screen, seperating top app bar form the main content
        topBar = {
            TopAppBar( // creates top bar with title and navigation button
                title = { Text("Add plant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PhotoSection(
                    photoUri = draft.photoUri,
                    busy = photoBusy,
                    error = photoError,
                    cameraAvailable = cameraAvailable,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onRemovePhoto = onRemovePhoto
                )

                if (isManual) { //shows an editable species name field when manual entry is enabled
                    OutlinedTextField( //allows the user to enter the plant's species name
                        value = draft.speciesName, //displays the current species name from the draft
                        onValueChange = onSpeciesNameChange, //allows user to update species name
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Plant species") },
                        placeholder = { Text("e.g. Peace Lily") },
                        isError = errors.species, //highlights the field when species validation fails
                        supportingText = { if (errors.species) Text("Enter the plant's name") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                } else {
                    Text(
                        //if species has been selected, display its name instead of asking user to edit it here
                        text = draft.speciesName.ifBlank { "Loading…" }, //show loading if its blank
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                //show current status of plant care info
                CareInfoStatus(careInfo)

                OutlinedTextField( //give input fields to change
                    value = draft.nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
                    label = { Text("Location (optional)") },
                    placeholder = { Text("e.g. Kitchen window") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = draft.wateringIntervalDays,
                    onValueChange = onIntervalChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
                    label = { Text("Light (optional)") },
                    placeholder = { Text("e.g. Bright indirect light") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save plant")
                }
            }
        }
    }
}

@Composable
private fun CareInfoStatus(state: CareInfoState) {
    when (state) { //selects UI to display based on current care information stare
        CareInfoState.Idle -> Unit // nothign is displayed while care information is idle

        CareInfoState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Fetching care info…", style = MaterialTheme.typography.bodySmall)
        } //displays loading indicator

        CareInfoState.Loaded -> Text(
            "Suggested watering and light filled in — adjust them if your plant needs something different.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is CareInfoState.Unavailable -> Text(
            buildString {
                state.error?.let { append(it.userMessage()).append(' ') }
                append("Couldn't load care info — please enter watering and light yourself.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    busy: Boolean,
    error: String?,
    cameraAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)                // keeps its shape at any width
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                Text(
                    "No photo yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (busy) CircularProgressIndicator()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onTakePhoto,
                enabled = cameraAvailable && !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Take photo") }
            OutlinedButton(
                onClick = onPickPhoto,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Gallery") }
        }

        if (photoUri != null) {
            TextButton(onClick = onRemovePhoto) { Text("Remove photo") }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
