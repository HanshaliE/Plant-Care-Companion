package com.example.plantcarecompanion.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.plantcarecompanion.data.local.AppDatabase
import com.example.plantcarecompanion.data.network.AndroidConnectivityChecker
import com.example.plantcarecompanion.data.network.RateLimitGuard
import com.example.plantcarecompanion.data.network.SharedPrefsRateLimitStore
import com.example.plantcarecompanion.data.photo.PlantPhotoStorage
import com.example.plantcarecompanion.data.remote.NetworkModule
import com.example.plantcarecompanion.data.repository.SpeciesRepository
import com.example.plantcarecompanion.ui.PlantCareViewModel
import com.example.plantcarecompanion.ui.addplant.AddPlantViewModel

object AppContainer {

    @Volatile
    private var speciesRepository: SpeciesRepository? = null

    fun speciesRepository(context: Context): SpeciesRepository =
        speciesRepository ?: synchronized(this) {
            speciesRepository ?: SpeciesRepository(
                api = NetworkModule.perenualApi,
                speciesDao = AppDatabase.get(context).speciesDao(),
                guard = RateLimitGuard(SharedPrefsRateLimitStore(context)),
                connectivity = AndroidConnectivityChecker(context),
                hasApiKey = NetworkModule.hasApiKey
            ).also { speciesRepository = it }
        }

    val addPlantViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as Application
            AddPlantViewModel(speciesRepository(app), PlantPhotoStorage(app))
        }
    }

    val plantCareViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as Application
            val db = AppDatabase.get(app)
            PlantCareViewModel(db.plantDao(), db.careLogDao())
        }
    }
}
