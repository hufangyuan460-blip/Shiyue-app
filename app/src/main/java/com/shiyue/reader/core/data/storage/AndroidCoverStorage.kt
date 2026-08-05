package com.shiyue.reader.core.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.CoverTransform
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidCoverStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CoverStorage {
    private val coversDir get() = File(context.filesDir, COVERS_DIRECTORY)
    private val captureDir get() = File(context.cacheDir, CAPTURE_DIRECTORY)
    private val cropDir get() = File(context.cacheDir, CROP_DIRECTORY)

    override suspend fun createCaptureTarget(): CaptureTarget = withContext(Dispatchers.IO) {
        captureDir.mkdirs()
        val file = File(captureDir, "${UUID.randomUUID()}.jpg")
        check(file.createNewFile()) { "Unable to create capture file" }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        CaptureTarget(uri.toString(), file.name)
    }

    override suspend fun processAndStoreCover(
        sourceUri: String,
        transform: CoverTransform,
    ): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val decoded = decodeSampled(uri)
        val oriented = applyExifOrientation(decoded, readExifOrientation(uri))
        if (oriented !== decoded) decoded.recycle()
        val rotated = rotate(oriented, Math.floorMod(transform.quarterTurns, 4) * 90)
        if (rotated !== oriented) oriented.recycle()
        val output = cropToCover(rotated, transform)
        rotated.recycle()
        coversDir.mkdirs()
        val relativePath = "$COVERS_DIRECTORY/${UUID.randomUUID()}.jpg"
        val destination = File(context.filesDir, relativePath)
        destination.outputStream().buffered().use { stream ->
            check(output.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "Unable to encode cover" }
        }
        output.recycle()
        relativePath
    }

    override suspend fun deleteCover(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        controlledCoverFile(relativePath)?.let { !it.exists() || it.delete() } ?: false
    }

    override suspend fun deleteTemporary(token: String) = withContext(Dispatchers.IO) {
        val target = File(captureDir, File(token).name)
        if (target.parentFile == captureDir) target.delete()
    }

    override suspend fun cleanupTemporaryFiles() = withContext(Dispatchers.IO) {
        listOf(captureDir, cropDir).forEach { directory ->
            directory.listFiles()?.forEach(File::delete)
        }
    }

    override suspend fun cleanupOrphanedCovers(referencedPaths: Set<String>) = withContext(Dispatchers.IO) {
        val safeReferences = referencedPaths.mapNotNull { controlledCoverFile(it)?.canonicalPath }.toSet()
        coversDir.listFiles()?.forEach { file ->
            if (file.canonicalPath !in safeReferences) file.delete()
        }
        Unit
    }

    private fun decodeSampled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "Unable to open cover image" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid cover image" }
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DECODE_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "Unable to open cover image" }
            checkNotNull(BitmapFactory.decodeStream(input, null, options)) { "Unable to decode cover image" }
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

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
            postRotate(degrees.toFloat())
        }, true)
    }

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

    private fun cropToCover(bitmap: Bitmap, transform: CoverTransform): Bitmap {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height
        val baseWidth: Float
        val baseHeight: Float
        if (sourceRatio > TARGET_RATIO) {
            baseHeight = bitmap.height.toFloat()
            baseWidth = baseHeight * TARGET_RATIO
        } else {
            baseWidth = bitmap.width.toFloat()
            baseHeight = baseWidth / TARGET_RATIO
        }
        val zoom = transform.scale.coerceIn(1f, 4f)
        val cropWidth = (baseWidth / zoom).coerceAtLeast(1f)
        val cropHeight = (baseHeight / zoom).coerceAtLeast(1f)
        val maxLeft = bitmap.width - cropWidth
        val maxTop = bitmap.height - cropHeight
        val left = ((maxLeft / 2f) + transform.offsetXFraction.coerceIn(-1f, 1f) * maxLeft / 2f)
            .coerceIn(0f, maxLeft)
        val top = ((maxTop / 2f) + transform.offsetYFraction.coerceIn(-1f, 1f) * maxTop / 2f)
            .coerceIn(0f, maxTop)
        val source = Rect(left.toInt(), top.toInt(), (left + cropWidth).toInt(), (top + cropHeight).toInt())
        val output = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, source, Rect(0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT), Paint(Paint.ANTI_ALIAS_FLAG))
        }
        return output
    }

    private fun controlledCoverFile(relativePath: String): File? {
        val file = File(context.filesDir, relativePath)
        val coversRoot = coversDir.canonicalFile
        return file.canonicalFile.takeIf { it.parentFile == coversRoot }
    }

    private companion object {
        const val COVERS_DIRECTORY = "covers"
        const val CAPTURE_DIRECTORY = "cover-capture"
        const val CROP_DIRECTORY = "cover-crop"
        const val MAX_DECODE_DIMENSION = 2400
        const val OUTPUT_WIDTH = 1200
        const val OUTPUT_HEIGHT = 1800
        const val TARGET_RATIO = 2f / 3f
    }
}
