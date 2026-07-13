package com.converty.app.i18n

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupResilienceContractTest {
    @Test
    fun `nonessential storage locale work and notification failures cannot kill first screen`() {
        val sourceRoot = sourceRoot()
        val viewModel = Files.readString(
            sourceRoot.resolve("com/converty/app/feature/app/ConvertyViewModel.kt"),
        )
        val locale = Files.readString(
            sourceRoot.resolve("com/converty/app/ui/theme/AppLocaleController.kt"),
        )
        val scheduler = Files.readString(
            sourceRoot.resolve("com/converty/app/work/ConversionScheduler.kt"),
        )
        val application = Files.readString(
            sourceRoot.resolve("com/converty/app/ConvertyApplication.kt"),
        )

        assertTrue(viewModel.contains("settingsRepository.settings.catch"))
        assertTrue(viewModel.contains("historyRepository.observeJobs().catch"))
        assertTrue(viewModel.contains("emit(AppSettings())"))
        assertTrue(viewModel.contains("emit(emptyList())"))
        assertTrue(locale.contains("runCatching"))
        assertTrue(scheduler.contains("private val workManager by lazy"))
        assertTrue(application.contains("runCatching { ConversionNotifications.createChannel(this) }"))
    }

    private fun sourceRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            val candidate = current.resolve("app/src/main/java")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent
        }
        error("Could not locate app/src/main/java above ${System.getProperty("user.dir")}")
    }
}
