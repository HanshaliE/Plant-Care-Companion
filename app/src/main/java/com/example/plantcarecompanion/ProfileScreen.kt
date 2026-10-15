package com.example.plantcarecompanion

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
    )
    var showEditSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.profileSaved) {
        if (viewModel.profileSaved) {
            snackbarHostState.showSnackbar("Profile saved")
            viewModel.dismissProfileSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(
                name = viewModel.name,
                bio = viewModel.bio,
                initial = viewModel.initial,
                photoPath = viewModel.photoPath,
                onEditClick = { showEditSheet = true }
            )

            ProfileStatsRow(
                plantCount = viewModel.plantCount,
                careActionCount = viewModel.careActionCount,
                remindersOn = viewModel.notificationsEnabled
            )

            AccountCard(
                notificationsEnabled = viewModel.notificationsEnabled,
                memberSinceLabel = viewModel.memberSinceLabel,
                onEditProfileClick = { showEditSheet = true }
            )

            AboutCard()
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            initialName = viewModel.name,
            initialBio = viewModel.bio,
            photoPath = viewModel.photoPath,
            onDismiss = { showEditSheet = false },
            onSave = { newName, newBio, pickedUri ->
                viewModel.saveProfile(newName, newBio, pickedUri)
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    bio: String,
    initial: String,
    photoPath: String?,
    onEditClick: () -> Unit
) {
    val bitmap = rememberProfileBitmap(photoPath)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            initial,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Edit profile photo" }
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            if (bio.isNotBlank()) {
                Text(
                    bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(plantCount: Int, careActionCount: Int, remindersOn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileStatTile(
            modifier = Modifier.weight(1f),
            emoji = "🪴",
            value = plantCount.toString(),
            label = "Plants"
        )
        ProfileStatTile(
            modifier = Modifier.weight(1f),
            emoji = "💧",
            value = careActionCount.toString(),
            label = "Care actions"
        )
        ProfileStatTile(
            modifier = Modifier.weight(1f),
            emoji = "🔔",
            value = if (remindersOn) "On" else "Off",
            label = "Reminders"
        )
    }
}

@Composable
private fun ProfileStatTile(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$emoji $value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AccountCard(
    notificationsEnabled: Boolean,
    memberSinceLabel: String,
    onEditProfileClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
            )

            AccountRow(label = "Edit profile", value = null, onClick = onEditProfileClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccountRow(
                label = "Notifications",
                value = if (notificationsEnabled) "On" else "Off",
                onClick = null
            )
            if (memberSinceLabel.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AccountRow(label = memberSinceLabel, value = null, onClick = null)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String?, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("About", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Your profile, plants, and care history are stored only on this device. " +
                    "When you look up a species, only its name is sent to the Perenual API to fetch " +
                    "care information — nothing else about you or your plants leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun rememberProfileBitmap(path: String?): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = if (path != null) {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            }
        } else {
            null
        }
    }
    return state.value
}

@Composable
internal fun rememberBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)
                        ?.use { BitmapFactory.decodeStream(it) }
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }
    return state.value
}
