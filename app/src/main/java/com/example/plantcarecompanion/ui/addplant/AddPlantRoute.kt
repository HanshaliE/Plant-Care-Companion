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

/**
 * The stateful wrapper around [AddPlantScreen] — the "owner" in state-hoisting terms.
 * It connects the screen to the ViewModel and to the two device features (camera, photo picker).
 */
@Composable
fun AddPlantRoute(
    speciesId: Int,
    viewModel: AddPlantViewModel,
    onSave: (Plant) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(speciesId) { viewModel.loadSpecies(speciesId) }

    // After a successful save the form is emptied once this screen has actually left the screen.
    DisposableEffect(Unit) {
        onDispose { viewModel.onAddPlantScreenDisposed() }
    }

    // System back = cancel this add (discard draft + any photo taken for it).
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val cameraAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // No CAMERA permission needed: the system camera app takes the photo and writes it to our file.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onCameraResult(success) }

    // Photo picker: no storage permission needed either.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onGalleryPicked(uri) }

    AddPlantScreen(
        draft = viewModel.draft,
        errors = viewModel.errors,
        careInfo = viewModel.careInfo,
        isManual = speciesId == MANUAL_ENTRY,
        photoBusy = viewModel.photoBusy,
        photoError = viewModel.photoError,
        cameraAvailable = cameraAvailable,
        onSpeciesNameChange = viewModel::onSpeciesNameChange,
        onNicknameChange = viewModel::onNicknameChange,
        onLocationChange = viewModel::onLocationChange,
        onIntervalChange = viewModel::onIntervalChange,
        onLightChange = viewModel::onLightChange,
        onTakePhoto = {
            try {
                cameraLauncher.launch(viewModel.prepareCameraShot())
            } catch (e: ActivityNotFoundException) {
                viewModel.onCameraUnavailable()
            }
        },
        onPickPhoto = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = viewModel::removePhoto,
        onSave = {
            val plant = viewModel.buildPlant()          // null if the form isn't valid (errors are shown)
            if (plant != null) {
                onSave(plant)                           // the caller stores it and navigates away
                viewModel.onPlantSaved()
            }
        },
        onBack = onBack
    )
}
