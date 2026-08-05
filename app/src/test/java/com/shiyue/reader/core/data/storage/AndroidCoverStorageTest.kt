package com.shiyue.reader.core.data.storage

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.domain.repository.CoverTransform
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
class AndroidCoverStorageTest {
    private lateinit var context: Context
    private lateinit var storage: AndroidCoverStorage
    private lateinit var source: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = AndroidCoverStorage(context)
        source = File(context.cacheDir, "cover-source.png")
        Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(80, 110, 90))
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
    }

    @After
    fun tearDown() {
        source.delete()
        File(context.filesDir, "covers").listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `processed cover has fixed two by three output and controlled deletion`() = runBlocking {
        val path = storage.processAndStoreCover(Uri.fromFile(source).toString(), CoverTransform(scale = 2f, quarterTurns = 1))
        val output = File(context.filesDir, path)
        val bitmap = BitmapFactory.decodeFile(output.absolutePath)

        assertEquals(1200, bitmap.width)
        assertEquals(1800, bitmap.height)
        bitmap.recycle()
        assertTrue(output.exists())
        assertTrue(storage.deleteCover(path))
        assertFalse(output.exists())
        assertFalse(storage.deleteCover("../outside.jpg"))
    }
}
