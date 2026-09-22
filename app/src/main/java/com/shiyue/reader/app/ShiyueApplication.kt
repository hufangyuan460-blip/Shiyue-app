package com.shiyue.reader.app

import android.app.Application
import com.shiyue.reader.domain.usecase.CleanupCoverFilesUseCase
import com.shiyue.reader.domain.usecase.CleanupNoteFilesUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShiyueApplication : Application() {
    @Inject lateinit var cleanupCoverFiles: CleanupCoverFilesUseCase
    @Inject lateinit var cleanupNoteFiles: CleanupNoteFilesUseCase
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runCatching { cleanupCoverFiles() } }
        applicationScope.launch { runCatching { cleanupNoteFiles() } }
    }
}
