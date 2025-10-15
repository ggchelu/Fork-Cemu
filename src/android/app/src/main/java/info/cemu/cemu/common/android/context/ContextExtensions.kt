package info.cemu.cemu.common.android.context

import android.content.Context
import android.os.Environment
import java.io.File

fun Context.internalFolder(): File {
    val externalFilesDir = getExternalFilesDir(null)
    if (externalFilesDir != null) {
        return externalFilesDir
    }
    return filesDir
}

fun Context.cemuExternalFolder(): File {
    return try {
        val externalStorage = Environment.getExternalStorageDirectory()
        File(externalStorage, "Emulation/storage/Cemu")
    } catch (e: Exception) {
        // Fallback to internal storage if external storage is not available
        File(internalFolder(), "Emulation/storage/Cemu")
    }
}