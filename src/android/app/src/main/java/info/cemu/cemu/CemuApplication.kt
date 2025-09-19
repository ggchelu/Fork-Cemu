package info.cemu.cemu

import android.app.Application
import info.cemu.cemu.common.android.context.internalFolder
import info.cemu.cemu.common.android.context.cemuExternalFolder
import info.cemu.cemu.common.ui.localization.setLanguage
import info.cemu.cemu.common.ui.localization.setTranslations
import info.cemu.cemu.nativeinterface.NativeActiveSettings.initializeActiveSettings
import info.cemu.cemu.nativeinterface.NativeActiveSettings.setInternalDir
import info.cemu.cemu.nativeinterface.NativeActiveSettings.setNativeLibDir
import info.cemu.cemu.nativeinterface.NativeEmulation.initializeEmulation
import info.cemu.cemu.nativeinterface.NativeEmulation.setDPI
import info.cemu.cemu.nativeinterface.NativeGraphicPacks.refreshGraphicPacks
import info.cemu.cemu.nativeinterface.NativeLogging.crashLog
import info.cemu.cemu.nativeinterface.NativeSwkbd.initializeSwkbd
import info.cemu.cemu.common.settings.SettingsManager
import info.cemu.cemu.nativeinterface.NativeFiles
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.regex.Pattern

class CemuApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        configureExceptionHandler()

        SettingsManager.initialize(this)

        NativeFiles.initialize(contentResolver)

        initializeTranslations()

        initializeCemu()

        saveDataFiles()
    }

    private fun initializeTranslations() {
        setTranslations(this)
        setLanguage(SettingsManager.guiSettings.language, this)
    }

    private fun saveDataFiles() {
        val dataFolder = File(internalCemuDataFolder)

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return
        }

        val hashFileName = "hash.txt"
        val hashFile = dataFolder.resolve(hashFileName)
        val oldHash = if (hashFile.isFile) hashFile.readText() else "invalid"

        val newHash = try {
            assets.open(hashFileName).use { it.reader().readText() }
        } catch (_: IOException) {
            return
        }

        if (oldHash == newHash) {
            return
        }

        dataFolder.deleteRecursively()
        dataFolder.mkdirs()
        dataFolder.resolve(hashFileName).writeText(newHash)

        fun traverseAssets(path: String = ""): Iterator<String> = iterator {
            val assetFiles = assets.list(path) ?: return@iterator

            if (assetFiles.isEmpty()) {
                yield(path)
            }

            for (assetFile in assetFiles) {
                val assetPath = path + (if (path == "") "" else "/") + assetFile
                for (file in traverseAssets(assetPath)) {
                    yield(file)
                }
            }
        }

        val filePatterns = arrayOf(
            Pattern.compile("gameProfiles/.*"),
            Pattern.compile("resources/.*"),
        )

        fun isFileValid(file: String): Boolean {
            return filePatterns.any { pattern -> pattern.matcher(file).matches() }
        }

        for (assetFile in traverseAssets()) {
            if (!isFileValid(assetFile)) {
                continue
            }

            val outFile = dataFolder.resolve(assetFile)
            outFile.parentFile?.mkdirs()
            assets.open(assetFile)
                .use { asset -> outFile.outputStream().use { out -> asset.copyTo(out) } }
        }
    }

    private fun configureExceptionHandler() {
        if (DefaultUncaughtExceptionHandler == null) {
            DefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        }
        Thread.setDefaultUncaughtExceptionHandler { thread: Thread, exception: Throwable ->
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            exception.printStackTrace(printWriter)
            val stacktrace = stringWriter.toString()
            crashLog(stacktrace)
            DefaultUncaughtExceptionHandler!!.uncaughtException(
                thread,
                exception
            )
        }
    }

    private fun initializeCemu() {
        try {
            val displayMetrics = resources.displayMetrics
            setDPI(displayMetrics.density)
            
            // Setup external Cemu directory
            setupExternalCemuDirectory()
            
            initializeActiveSettings(
                userDataPath = externalCemuUserFolder,
                dataPath = internalCemuDataFolder,
                cachePath = externalCemuUserFolder,
            )
            setNativeLibDir(applicationInfo.nativeLibraryDir)
            setInternalDir(dataDir.absolutePath)
            initializeEmulation()
            initializeSwkbd()
            refreshGraphicPacks()
        } catch (e: Exception) {
            android.util.Log.e("CemuApplication", "Failed to initialize Cemu: ${e.message}", e)
            // Continue app execution with minimal functionality
        }
    }

    private val internalCemuDataFolder: String
        get() = internalFolder().resolve("data").toString()

    private val internalCemuUserFolder: String
        get() = internalFolder().toString()

    private val externalCemuUserFolder: String
        get() = try {
            // Try external storage first, fallback to internal
            val externalDir = cemuExternalFolder()
            if (externalDir.canWrite() && canWriteToExternalStorage()) {
                externalDir.toString()
            } else {
                internalFolder().toString()
            }
        } catch (e: Exception) {
            internalFolder().toString()
        }

    private fun canWriteToExternalStorage(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun setupExternalCemuDirectory() {
        try {
            val externalCemuDir = cemuExternalFolder()
            android.util.Log.i("CemuApplication", "Setting up external directory: ${externalCemuDir.absolutePath}")
            android.util.Log.i("CemuApplication", "Can write: ${externalCemuDir.canWrite()}, Can write to external storage: ${canWriteToExternalStorage()}")
            
            if (externalCemuDir.canWrite() && canWriteToExternalStorage()) {
                if (!externalCemuDir.exists()) {
                    val created = externalCemuDir.mkdirs()
                    android.util.Log.i("CemuApplication", "Created external directory: $created")
                } else {
                    android.util.Log.i("CemuApplication", "External directory already exists")
                }
                
                // Migrate existing settings.xml if it exists in internal storage
                val internalSettingsFile = File(internalFolder(), "settings.xml")
                val externalSettingsFile = File(externalCemuDir, "settings.xml")
                
                android.util.Log.i("CemuApplication", "Internal settings exists: ${internalSettingsFile.exists()}, External settings exists: ${externalSettingsFile.exists()}")
                
                if (internalSettingsFile.exists() && !externalSettingsFile.exists()) {
                    internalSettingsFile.copyTo(externalSettingsFile, overwrite = false)
                    android.util.Log.i("CemuApplication", "Migrated settings.xml to external storage")
                }
            } else {
                android.util.Log.w("CemuApplication", "Cannot write to external storage, using internal storage")
            }
        } catch (e: Exception) {
            android.util.Log.w("CemuApplication", "External storage not available, using internal: ${e.message}")
        }
    }

    companion object {
        init {
            try {
                System.loadLibrary("CemuAndroid")
            } catch (e: Exception) {
                android.util.Log.e("CemuApplication", "Failed to load CemuAndroid library: ${e.message}", e)
            }
        }

        private var DefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    }
}
