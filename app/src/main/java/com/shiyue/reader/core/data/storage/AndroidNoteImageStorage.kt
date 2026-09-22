package com.shiyue.reader.core.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.NoteImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidNoteImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NoteImageStorage {
    private val notesDir get() = File(context.filesDir, NOTES_DIRECTORY)
    private val captureDir get() = File(context.cacheDir, CAPTURE_DIRECTORY)

    override suspend fun createCaptureTarget(): CaptureTarget = withContext(Dispatchers.IO) {
        captureDir.mkdirs()
        val file = File(captureDir, "${UUID.randomUUID()}.jpg")
        check(file.createNewFile()) { "Unable to create note capture file" }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        CaptureTarget(uri.toString(), file.name)
    }

    override suspend fun processAndStoreImage(sourceUri: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val decoded = decodeSampled(uri)
        val oriented = applyExifOrientation(decoded, readExifOrientation(uri))
        if (oriented !== decoded) decoded.recycle()
        notesDir.mkdirs()
        val relativePath = "$NOTES_DIRECTORY/${UUID.randomUUID()}.jpg"
        val destination = File(context.filesDir, relativePath)
        destination.outputStream().buffered().use { stream ->
            check(oriented.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "Unable to encode note image" }
        }
        oriented.recycle()
        relativePath
    }

    override suspend fun deleteImage(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        controlledNoteFile(relativePath)?.let { !it.exists() || it.delete() } ?: false
    }

    override suspend fun deleteTemporary(token: String) = withContext(Dispatchers.IO) {
        val target = File(captureDir, File(token).name)
        if (target.parentFile == captureDir) target.delete()
    }

    override suspend fun cleanupTemporaryFiles() = withContext(Dispatchers.IO) {
        captureDir.listFiles()?.forEach(File::delete)
        Unit
    }

    override suspend fun cleanupOrphanedImages(referencedPaths: Set<String>) = withContext(Dispatchers.IO) {
        val safeReferences = referencedPaths.mapNotNull { controlledNoteFile(it)?.canonicalPath }.toSet()
        notesDir.listFiles()?.forEach { file ->
            if (file.canonicalPath !in safeReferences) file.delete()
        }
        Unit
    }

    private fun decodeSampled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "Unable to open note image" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid note image" }
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DECODE_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "Unable to open note image" }
            checkNotNull(BitmapFactory.decodeStream(input, null, options)) { "Unable to decode note image" }
        }
    }

    private fun readExifOrientation(uri: Uri): Int = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun controlledNoteFile(relativePath: String): File? {
        val file = File(context.filesDir, relativePath)
        val notesRoot = notesDir.canonicalFile
        return file.canonicalFile.takeIf { it.parentFile == notesRoot }
    }

    private companion object {
        const val NOTES_DIRECTORY = "notes"
        const val CAPTURE_DIRECTORY = "note-capture"
        const val MAX_DECODE_DIMENSION = 2400
    }
}
