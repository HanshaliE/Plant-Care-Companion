package com.example.plantcarecompanion.ui.addplant

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.domain.MANUAL_ENTRY

//connects add plant screen ui to viewmodel and handles navigation, loading species details, selecting plant photos
@Composable
fun AddPlantRoute(
    speciesId: Int,
    viewModel: AddPlantViewModel,
    onSave: (Plant) -> Unit,
    onBack: () -> Unit
) {
    //loads selected species details whenever speciesID changes
    LaunchedEffect(speciesId) { viewModel.loadSpecies(speciesId) }

    //registers cleanup logic when this screen leaves composition.
    DisposableEffect(Unit) { //unit is a contant key so this effect is not restarted just because unrealted state changes
        onDispose { viewModel.onAddPlantScreenDisposed() } //when screen leaves composition, viewmodel can cleean up temporary resources or state associated with the screen.
    }

    //intercepts the device's back instead of relying on default behaviour
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val cameraAvailable = remember { //checks if device has any camera hardware and caches the result to remember
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val cameraLauncher = rememberLauncherForActivityResult( //creates a laucher for selecting an image using device camera
        ActivityResultContracts.TakePicture() //takepicture() returns a boolean indicating whether pciture taking was successful or not and saved to the URI supplied when lauching it
    ) { success -> viewModel.onCameraResult(success) }

    val galleryLauncher = rememberLauncherForActivityResult( // creates a launcheer for selecting an image using android's photo pciker.
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onGalleryPicked(uri) }

    //displays athe add plan screen and connects ui elements to the state and funtions provided by the viewmodel
    AddPlantScreen(
        draft = viewModel.draft, //current plant form data
        errors = viewModel.errors, // validation errors that should be shown to user
        careInfo = viewModel.careInfo, //care information for the selected plant species
        isManual = speciesId == MANUAL_ENTRY, //determines whetehr the screen is in manual entry mode
        photoBusy = viewModel.photoBusy, //check if photo operation is currently in progress
        photoError = viewModel.photoError, //error message related to taking or selecting a photo
        cameraAvailable = cameraAvailable, // tells the screen whether camera hardware is available
        onSpeciesNameChange = viewModel::onSpeciesNameChange,
        onNicknameChange = viewModel::onNicknameChange,
        onLocationChange = viewModel::onLocationChange,
        onIntervalChange = viewModel::onIntervalChange,
        onLightChange = viewModel::onLightChange,
        onTakePhoto = {
            try { // attaempt to launch the camera with URI given by viewmodel
                cameraLauncher.launch(viewModel.prepareCameraShot())
            } catch (e: ActivityNotFoundException) { //handle the case where the required camera activity cannot be found
                viewModel.onCameraUnavailable() //informs the viewmodel that the camera is unavailable
            }
        },
        onPickPhoto = {
            galleryLauncher.launch( //opens the system photo picker and restricts selection to images
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = viewModel::removePhoto, //passes the viewmodel function
        onSave = {
            val plant = viewModel.buildPlant() // check the current form data and try to build a plant object, return null if cannot
            if (plant != null) {
                onSave(plant) //pass completed plant object to caller
                viewModel.onPlantSaved()
            }
        },
        onBack = onBack
    )
}
