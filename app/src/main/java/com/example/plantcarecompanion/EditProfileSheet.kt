package com.example.plantcarecompanion

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts  //provides predefined ways to interact with android activities
import androidx.compose.foundation.Image //displays a bitmap image in the UI
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row //Column, Row, Box - Arrange UI elements vertically, horizontally or on top of each other
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet //displays a panel that slides up from the bottom of the screen
import androidx.compose.material3.OutlinedTextField //creates editable text input fields
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier //Controls layout, size, padding, shape, and other visual properties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)  //ExperimentalMaterial3Api - its experimental but we are using it anyways using OptIn for "ModalBottomSheet"
@Composable //tells jetpack compose that the function describes UI
fun EditProfileSheet(
    initialName: String,
    initialBio: String,
    photoPath: String?,
    photoError: String?,
    onPrepareCameraShot: () -> Uri,
    onCameraResult: (Boolean) -> String?,
    onCameraUnavailable: () -> Unit,
    onDiscardPendingCameraShot: (capturedPhotoPath: String?) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, pickedGalleryUri: Uri?, capturedPhotoPath: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var bio by rememberSaveable { mutableStateOf(initialBio) }
    // At most one of these is non-null at a time: a gallery pick (content:// Uri we don't own,
    // copied into internal storage only on Save) or a camera capture (a file we already own on
    // disk the moment the camera app returns).
    var pickedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) } // guards the dispose-cleanup below

    // If the sheet closes without the user tapping Save, throw away any photo already
    // captured to disk so it doesn't linger as an orphaned file.
    DisposableEffect(Unit) {
        onDispose {
            if (!saved) onDiscardPendingCameraShot(capturedPhotoPath)
        }
    }

    val context = LocalContext.current
    val cameraAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // No CAMERA permission needed: the system camera app takes the photo and writes it to our file.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = onCameraResult(success)
        if (path != null) {
            capturedPhotoPath = path
            pickedUri = null // a capture replaces any earlier gallery pick
        }
        // path == null means the user cancelled the camera app, or it failed — leave everything as is.
    }

    val pickMedia = rememberLauncherForActivityResult( //rememberLauncherForActivityResult creates and remembers a launcher
        ActivityResultContracts.PickVisualMedia() //PickVisualMedia specifies that the app wants to let the user select visual media
        //andriod opens the photo picker when the launcher is activated
    ) { uri ->
        //only update the selected URI if the user actually selects an image. If they cancel - uri can be null
        if (uri != null) {
            // Replacing a not-yet-saved camera capture with a gallery pick: drop the unused file.
            if (capturedPhotoPath != null) {
                onDiscardPendingCameraShot(capturedPhotoPath)
                capturedPhotoPath = null
            }
            pickedUri = uri
        }
    }

    val pickedBitmap = rememberBitmapFromUri(pickedUri)
    val capturedBitmap = rememberProfileBitmap(capturedPhotoPath)
    val savedBitmap = rememberProfileBitmap(photoPath)
    // Prefer whatever was just picked/captured this session over the already-saved photo.
    val previewBitmap = capturedBitmap ?: pickedBitmap ?: savedBitmap

    //creates a bottom sheet that slides up from the bottom of the screen (For UI enhancement)
    ModalBottomSheet(
        onDismissRequest = onDismiss, //calls the parent's dismiss callback when the sheet is dismissed.
        sheetState = rememberModalBottomSheetState() //remembers the bottom sheet's current state
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        saved = true
                        onSave(name, bio, pickedUri, capturedPhotoPath)
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { //creates a bix that allows the camera button to be positioned at the bottom right corner
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box( //creates a circular profile picture area
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape) // clips the content into a circle
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        //checks whether the image is available for display
                        if (previewBitmap != null) {
                            Image( //displays the selected or saved profile photo
                                bitmap = previewBitmap,
                                contentDescription = "Profile photo", //describes the image for accessibility
                                contentScale = ContentScale.Crop, //crops the image to full the circular area without stretching its proportions
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                //removes surrounding whitespaces and gets the first character if one exists and converts it to a string upperclass. Uses "P" if no character exists
                                name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { showPhotoSourceDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Change profile photo" }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📷", style = MaterialTheme.typography.labelMedium, color = Color.White) //displays camera emoji. button opens photo picker. not an actual camera
                        }
                    }
                }
            }

            if (photoError != null) {
                Text(
                    photoError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            //bio text field
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Update photo") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            try {
                                cameraLauncher.launch(onPrepareCameraShot())
                            } catch (e: ActivityNotFoundException) {
                                onCameraUnavailable()
                            }
                        },
                        enabled = cameraAvailable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Take photo")
                    }
                    TextButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose from gallery")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
