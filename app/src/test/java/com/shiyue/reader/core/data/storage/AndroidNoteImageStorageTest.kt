package com.shiyue.reader.core.data.storage

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class AndroidNoteImageStorageTest {
    private lateinit var context: Context
    private lateinit var storage: AndroidNoteImageStorage
    private lateinit var source: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = AndroidNoteImageStorage(context)
        source = File(context.cacheDir, "note-source.png")
        Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(80, 110, 90))
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
    }

    @After
    fun tearDown() {
        source.delete()
        File(context.filesDir, "notes").listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `process stores a decodable jpeg and deletion is controlled`() = runBlocking {
        val path = storage.processAndStoreImage(Uri.fromFile(source).toString())
        val output = File(context.filesDir, path)
        val bitmap = BitmapFactory.decodeFile(output.absolutePath)
        assertEquals(600, bitmap.width)
        assertEquals(400, bitmap.height)
        bitmap.recycle()
        assertTrue(output.exists())
        assertTrue(storage.deleteImage(path))
        assertFalse(output.exists())
        assertFalse(storage.deleteImage("../outside.jpg"))
    }

    @Test
    fun `orphan cleanup keeps referenced and removes the rest`() = runBlocking {
        val kept = storage.processAndStoreImage(Uri.fromFile(source).toString())
        val orphan = storage.processAndStoreImage(Uri.fromFile(source).toString())
        storage.cleanupOrphanedImages(setOf(kept))
        assertTrue(File(context.filesDir, kept).exists())
        assertFalse(File(context.filesDir, orphan).exists())
    }
}
