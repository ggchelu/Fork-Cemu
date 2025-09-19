package info.cemu.cemu

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import info.cemu.cemu.about.AboutCemuRoute
import info.cemu.cemu.about.aboutCemuNavigation
import info.cemu.cemu.common.ui.components.ActivityContent
import info.cemu.cemu.common.ui.localization.TranslatableContent
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.emulation.EmulationActivity
import info.cemu.cemu.gamelist.GameListRoute
import info.cemu.cemu.gamelist.gameListNavigation
import info.cemu.cemu.graphicpacks.GraphicPacksRoute
import info.cemu.cemu.graphicpacks.graphicPacksNavigation
import info.cemu.cemu.nativeinterface.NativeActiveSettings
import info.cemu.cemu.nativeinterface.NativeGameTitles.Game
import info.cemu.cemu.nativeinterface.NativeSettings
import info.cemu.cemu.provider.DocumentsProvider
import info.cemu.cemu.settings.SettingsRoute
import info.cemu.cemu.settings.settingsNavigation
import info.cemu.cemu.titlemanager.TitleManagerRoute
import info.cemu.cemu.titlemanager.titleManagerNavigation
import java.io.File

import android.graphics.drawable.Icon as AndroidIcon


class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled, app will continue with fallback to internal storage if needed
    }
    
    private val requestManageExternalStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle manage external storage permission result
    }
    
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use MANAGE_EXTERNAL_STORAGE
            if (Environment.isExternalStorageManager()) {
                // Already have permission
                android.util.Log.i("MainActivity", "External storage manager permission already granted")
                return
            }
            
            try {
                android.util.Log.i("MainActivity", "Requesting MANAGE_EXTERNAL_STORAGE permission")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestManageExternalStorageLauncher.launch(intent)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to request MANAGE_EXTERNAL_STORAGE, falling back to legacy: ${e.message}")
                // Fallback to legacy permissions
                requestLegacyStoragePermissions()
            }
        } else {
            // Android 10 and below - Use legacy permissions
            android.util.Log.i("MainActivity", "Using legacy storage permissions for Android ${Build.VERSION.SDK_INT}")
            requestLegacyStoragePermissions()
        }
    }
    
    private fun requestLegacyStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            
            if (permissions.isNotEmpty()) {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request storage permissions for external storage access
        requestStoragePermissions()
        
        setContent {
            TranslatableContent {
                ActivityContent {
                    MainNav()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NativeSettings.saveSettings()
    }

    override fun onPause() {
        super.onPause()
        NativeSettings.saveSettings()
    }
}

@Composable
private fun MainNav() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = GameListRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        gameListNavigation(
            navController = navController,
            startGame = { startGame(context, it) },
            createShortcut = { createShortcutForGame(context, it) }
        ) {
            GameListToolBarActionsMenu(
                goToSettings = { navController.navigate(SettingsRoute) },
                goToTitleManager = { navController.navigate(TitleManagerRoute) },
                goToGraphicPacks = { navController.navigate(GraphicPacksRoute) },
                goToAboutCemu = { navController.navigate(AboutCemuRoute) }
            )
        }
        settingsNavigation(navController)
        titleManagerNavigation(navController)
        graphicPacksNavigation(navController)
        aboutCemuNavigation(navController)
    }
}

@Composable
private fun GameListToolBarActionsMenu(
    goToSettings: () -> Unit,
    goToTitleManager: () -> Unit,
    goToGraphicPacks: () -> Unit,
    goToAboutCemu: () -> Unit,
) {
    var expandMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    @Composable
    fun DropdownMenuItem(onClick: () -> Unit, text: String) {
        DropdownMenuItem(
            onClick = {
                onClick()
                expandMenu = false
            },
            text = { Text(text) },
        )
    }
    IconButton(
        modifier = Modifier.padding(end = 8.dp),
        onClick = { expandMenu = true },
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null
        )
    }
    DropdownMenu(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false }
    ) {
        DropdownMenuItem(
            onClick = goToSettings,
            text = tr("Settings")
        )
        DropdownMenuItem(
            onClick = goToGraphicPacks,
            text = tr("Graphic packs")
        )
        DropdownMenuItem(
            onClick = goToTitleManager,
            text = tr("Title manager")
        )
        DropdownMenuItem(
            onClick = { openCemuFolder(context) },
            text = tr("Open Cemu folder")
        )
        DropdownMenuItem(
            onClick = { shareLogFile(context) },
            text = tr("Share log file"),
        )
        DropdownMenuItem(
            onClick = goToAboutCemu,
            text = tr("About Cemu"),
        )
    }
}

private fun startGame(context: Context, game: Game) {
    Intent(
        context,
        EmulationActivity::class.java
    ).apply {
        putExtra(EmulationActivity.EXTRA_LAUNCH_PATH, game.path)
        context.startActivity(this)
    }
}

private fun shareLogFile(context: Context) {
    val logFileName = "log.txt"
    val logFile = File(NativeActiveSettings.getUserDataPath()).resolve(logFileName)

    if (!logFile.isFile) {
        Toast.makeText(context, tr("Log file doesn't exist"), Toast.LENGTH_LONG).show()
        return
    }

    val fileUri = DocumentsContract.buildDocumentUri(
        DocumentsProvider.AUTHORITY,
        DocumentsProvider.ROOT_ID + "/$logFileName"
    )

    val documentFile = DocumentFile.fromSingleUri(context, fileUri) ?: return

    val intent = Intent(Intent.ACTION_SEND)
        .setDataAndType(documentFile.uri, "text/plain")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .putExtra(Intent.EXTRA_STREAM, documentFile.uri)

    context.startActivity(Intent.createChooser(intent, null))
}

private fun openCemuFolder(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        intent.data = DocumentsContract.buildRootUri(
            DocumentsProvider.AUTHORITY,
            DocumentsProvider.ROOT_ID
        )
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, tr("Could not open Cemu folder"), Toast.LENGTH_LONG).show()
    }
}


private fun createShortcutForGame(
    context: Context,
    game: Game,
) {
    fun onFailedToCreateShortcut() {
        Toast.makeText(context, tr("Couldn't create shortcut for game"), Toast.LENGTH_LONG).show()
    }

    try {
        val shortcutManager = context.getSystemService(
            ShortcutManager::class.java
        )
        if (!shortcutManager.isRequestPinShortcutSupported) {
            onFailedToCreateShortcut()
            return
        }

        val icon = game.icon?.asAndroidBitmap().let {
            if (it != null) AndroidIcon.createWithBitmap(it)
            else AndroidIcon.createWithResource(context, R.mipmap.ic_launcher)
        }

        val intent = Intent(
            context,
            EmulationActivity::class.java
        )
        intent.action = Intent.ACTION_VIEW
        intent.putExtra(EmulationActivity.EXTRA_LAUNCH_PATH, game.path)

        val pinShortcutInfo = ShortcutInfo.Builder(context, game.titleId.toString())
            .setShortLabel(game.name!!)
            .setIntent(intent)
            .setIcon(icon)
            .build()

        val pinnedShortcutCallbackIntent =
            shortcutManager.createShortcutResultIntent(pinShortcutInfo)

        val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            pinnedShortcutCallbackIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
    } catch (_: Exception) {
        onFailedToCreateShortcut()
    }
}
