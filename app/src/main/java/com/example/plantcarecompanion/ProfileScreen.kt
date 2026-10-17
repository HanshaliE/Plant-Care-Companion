package com.example.plantcarecompanion

import android.graphics.BitmapFactory //used to load an image from the file stored on the device
import android.net.Uri //Uri represents the location/reference of the selected image
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
fun ProfileScreen() { //this is the main screen that display the user's profile
    val context = LocalContext.current //gets the android context for the current screen. Context provides access to android features, resources and files

    //creates or retreives the profileviewmodel. the viewmodel manages the profile data and business logic seperately from the UI
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context) //ProfileViewModelFactory tells android how to create this viewmodel
    )

    //tracks whether the edit profile bottom sheet should be displayed. false: hidden, true = visible
    var showEditSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() } // manages messages briefly displayed at the bottom of the screen such as profile saved

    LaunchedEffect(viewModel.profileSaved) { //LaunchedEffect runs a coroutine when its key changes. Here the key is viewModel.profileSaved. when profileSaved becomes true, a success message is displayed
        if (viewModel.profileSaved) {
            snackbarHostState.showSnackbar("Profile saved")  //displays temporary message
            viewModel.dismissProfileSaved() //tells viewmodel to reset the saved status flag. this prevents the same success message from appearing again unnecessarily
        }
    }

    Scaffold( //scaffold provides a standard screen structure. SnackbarHost parameter defines where snackbar messages appear.
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier //applies padding supplied by scaffold, preventing content from overllapping areas reserved by screen layout
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

            //displays account-related information and the edit profile row
            AccountCard(
                notificationsEnabled = viewModel.notificationsEnabled,
                memberSinceLabel = viewModel.memberSinceLabel,
                onEditProfileClick = { showEditSheet = true } //clicking edit profile opens the same bottom sheet
            )

            AboutCard() //displays the app's information and privacy description
        }
    }

    if (showEditSheet) { //only creates and displays the edit sheet when showEditSheet is true
        EditProfileSheet( //Opens EditProfileSheet composable
            initialName = viewModel.name, //supplies the current profile values to populate the form
            initialBio = viewModel.bio,
            photoPath = viewModel.photoPath,
            photoError = viewModel.photoError,
            onPrepareCameraShot = viewModel::prepareCameraShot,
            onCameraResult = viewModel::onCameraResult,
            onCameraUnavailable = viewModel::onCameraUnavailable,
            onDiscardPendingCameraShot = viewModel::discardPendingCameraShot,
            onDismiss = { showEditSheet = false },
            onSave = { newName, newBio, pickedUri, capturedPhotoPath ->
                viewModel.saveProfile(newName, newBio, pickedUri, capturedPhotoPath)
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun ProfileHeader( // private - it is meant to be used within this Kotlin file
    name: String,
    bio: String,
    initial: String,
    photoPath: String?,
    onEditClick: () -> Unit
) {
    val bitmap = rememberProfileBitmap(photoPath) //loads the saved profile photo from its file path.
    //The reult is an ImageBitmap that Compose can display. If photoPath is null or the image cannot be loaded, the result is null.

    Box( //Creates the background container for the profile header
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)) //rounds the corners of the header
            .background( //creates a gradient background from the primary colour to tertiary colour defined in the app's theme
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
                    if (bitmap != null) { //if bitmap is loaded successfully, display the photo
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop, //crops the image to fill the circular area
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        //if there is no usable phoyo, display the user's initial as a fallback instead
                        Text(
                            initial,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                //button positioned over the bottom right of the photo
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(48.dp)
                        //provides a descriptive label for accessibility tools
                        .semantics { contentDescription = "Edit profile photo" }
                ) {
                    //creates the small circular background for the pencil emoji
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

            //display users name
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            //only displays the bio if it contains non-whitespace text. isNotBlack() returns false for an empty string or whitespace
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

//Displays three statistical tiles side by side
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

@Composable  //creates one reusable statistic tile to avoid writing the same UI three times
private fun ProfileStatTile(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$emoji $value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) //Displays the emoji and value together
            Text(
                label, //displays the description beneath the value
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

//Displays the Account card - edit profile, notifications, memberSinceLabel
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

            AccountRow(label = "Edit profile", value = null, onClick = onEditProfileClick) //clickable row that opens the edit profile sheet. value = null - no right side value to display
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))//horizontal line separating account rows
            AccountRow( //displays whether notifications are enabled.
                label = "Notifications",
                value = if (notificationsEnabled) "On" else "Off",
                onClick = null //row is not clickable
            )
            if (memberSinceLabel.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AccountRow(label = memberSinceLabel, value = null, onClick = null) //display membership info
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

//a reusable row for displaying account info
@Composable
private fun AccountRow(label: String, value: String?, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) // .then() adds another Modifier to the existing modifier chain. If onClick is not null, add click behaviour, otherwise add modifier (no extra behaviour)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, //places the label on the left and any value on the right
        verticalAlignment = Alignment.CenterVertically
    ) {
        //displays the row's main label
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (value != null) { //displays a value if there is a value
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//Displays the information about the app and how it handles user data
@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("About", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) //card heading
            Text( //explains the app's stated data-handling behaviour
                "Your profile, plants, and care history are stored only on this device. " +
                    "When you look up a species, only its name is sent to the Perenual API to fetch " +
                    "care information — nothing else about you or your plants leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//loads the saved profile photo from the file path and returns it as an ImageBitmap
@Composable
internal fun rememberProfileBitmap(path: String?): ImageBitmap? { //path is nullable because the user might not have a saved photo
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path) { //initialValue = null because image starts as unavailable while loading
        //produceState creates observable state and runs a coroutine to produce its value
        value = if (path != null) {  //if path exists, load the image, otherwise keep its as null
            withContext(Dispatchers.IO) { // Dispatchers.IO for reading files. withContext moves the file reading work off the main UI thread
                BitmapFactory.decodeFile(path)?.asImageBitmap() // decodeFile reads the image file and creates a Bitmap.
                // ?. means only continue if value on the left is not null
            }
        } else {
            null //if decode fails, decodeFile return null
        }
    }
    return state.value //return the current state value. Initially it will be null, after loading it will contain the image
}

//loads an image using content URI, such as one returned by Android's photo picker
@Composable
internal fun rememberBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current //gets android context so we can access contentResolver
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = uri) { //loads the image asynchronously and exposes the result as Compose state. If uri changes, the loading block runs again.
        value = if (uri != null) { // if uri is provided try to load the image
            withContext(Dispatchers.IO) { //image reading is performed on the IO dispatcher so the main UI thread can remain responsive
                try {
                    context.contentResolver.openInputStream(uri) //openInputStream() opens a stream for reading the image refered to by the URI
                        ?.use { BitmapFactory.decodeStream(it) } // use automatically close stream when finished
                        ?.asImageBitmap() //converts that Bitmap for use in compose
                } catch (e: Exception) {
                    null // if the URI cannot be opened or the image cannot be read, return null instead of letting the exception crash this code
                }
            }
        } else {
            null //no URI provided, no image to load
        }
    }
    return state.value //returns the image currently held in the state
}
