package com.example.plantcarecompanion.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class PlantPhotoStorage(context: Context) {

    private val appContext = context.applicationContext

    private val dir: File
        get() = File(appContext.filesDir, "plant_photos").also { it.mkdirs() }

    fun newPhotoFile(): File {
        val file = File(dir, "plant_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
        file.createNewFile()
        return file
    }

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    suspend fun importFromGallery(source: Uri): File? = withContext(Dispatchers.IO) {
        val target = newPhotoFile()
        try {
            val input = appContext.contentResolver.openInputStream(source)
            if (input == null) {
                target.delete()
                null
            } else {
                input.use { stream -> target.outputStream().use { out -> stream.copyTo(out) } }
                target
            }
        } catch (e: IOException) {
            target.delete()
            null
        } catch (e: SecurityException) {
            target.delete()
            null
        }
    }

    fun uriString(file: File): String = file.toUri().toString()

    fun deleteByUriString(uriString: String?) {
        if (uriString == null) return
        val path = uriString.toUri().path ?: return
        val file = File(path)
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }
}
