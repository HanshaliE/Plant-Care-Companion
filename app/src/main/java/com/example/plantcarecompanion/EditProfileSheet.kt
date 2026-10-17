package com.example.plantcarecompanion

import android.net.Uri //represents the location or reference of a selected image.
import androidx.activity.compose.rememberLauncherForActivityResult // opens photo picker and receives its result
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet //displays a panel that slides up from the bottom of the screen
import androidx.compose.material3.OutlinedTextField //creates editable text input fields
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable //retains UI state across recompositions and supported configuration changes
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier //Controls layout, size, padding, shape, and other visual properties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)  //ExperimentalMaterial3Api - its experimental but we are using it anyways using OptIn for "ModalBottomSheet"
@Composable //tells jetpack compose that the function describes UI
fun EditProfileSheet(
    initialName: String,
    initialBio: String,
    photoPath: String?, //it can be null
    onDismiss: () -> Unit, // a callback that tell the parent screen to close the sheet
    onSave: (name: String, bio: String, pickedPhotoUri: Uri?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var bio by rememberSaveable { mutableStateOf(initialBio) }
    var pickedUri by rememberSaveable { mutableStateOf<Uri?>(null) } //rememberSavable survives screen rotation

    val pickMedia = rememberLauncherForActivityResult( //rememberLauncherForActivityResult creates and remembers a launcher
        ActivityResultContracts.PickVisualMedia() //PickVisualMedia specifies that the app wants to let the user select visual media
        //andriod opens the photo picker when the launcher is activated
    ) { uri ->
        //only update the selected URI if the user actually selects an image. If they cancel - uri can be null
        if (uri != null) {
            pickedUri = uri
        }
    }

    val pickedBitmap = rememberBitmapFromUri(pickedUri) //rememberBitmapFrontUri is a helper function defined elsewhere
    val savedBitmap = rememberProfileBitmap(photoPath) // loads the currently saved profile photo as a bitmap

    //chooses whih image to display. If pickedBitmap is not null, use the newly selected image. Otherwise, use  the existing saved image.
    val previewBitmap = pickedBitmap ?: savedBitmap

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
                    onClick = { onSave(name, bio, pickedUri) },
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
                        onClick = {
                            pickMedia.launch( //opens android photo picker when clicked
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly) //requests image only selected. videos are excluded
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Choose profile photo" } //description for accessibility
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

            Spacer(modifier = Modifier.height(16.dp)) //adds 16dp of vertical space below the profile picture
            //name text field
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
}
