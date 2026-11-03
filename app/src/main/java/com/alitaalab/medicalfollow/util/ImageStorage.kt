package com.alitaalab.medicalfollow.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Stores prescription photos inside the app's private storage. */
object ImageStorage {
    private fun dir(context: Context): File =
        File(context.filesDir, "prescriptions").apply { mkdirs() }

    fun newImageFile(context: Context): File = File(dir(context), "rx_${UUID.randomUUID()}.jpg")

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun copyToInternal(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = newImageFile(context)
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
