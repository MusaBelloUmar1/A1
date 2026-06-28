package com.musx.a1.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showLyrics by viewModel.showLyrics.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addFolder(uri.toString())
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val db = com.musx.a1.data.AppDatabase.getDatabase(context)
                val repository = com.musx.a1.repository.AppRepository(
                    db.bookDao(),
                    db.folderDao(),
                    db.playlistDao(),
                    db.progressDao(),
                    db.chapterDao(),
                    db.bookmarkDao()
                )
                val scanner = com.musx.a1.engine.scanner.FolderScanner(
                    context,
                    repository,
                    com.musx.a1.engine.PdfParser(context)
                )
                scanner.scanFolder(uri.toString())
            }
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItemDesign("Manage Folders", Icons.Default.Folder, onClick = { launcher.launch(null) })
            SettingsItemDesign("Sleep Timer", Icons.Default.Timer, "Off", onClick = { showSleepDialog = true })
            SettingsItemDesign("Playback Speed", Icons.Default.Speed, "1.0x", onClick = { showSpeedDialog = true })
            SettingsItemDesign(
                "Show Lyrics",
                Icons.Default.Lyrics,
                isSwitch = true,
                switchChecked = showLyrics,
                onCheckedChange = { viewModel.toggleLyrics(it) }
            )
            SettingsItemDesign(
                "Auto Play Next",
                Icons.Default.SkipNext,
                isSwitch = true,
                switchChecked = autoPlayNext,
                onCheckedChange = { viewModel.toggleAutoPlay(it) }
            )
            SettingsItemDesign("Clear Cache", Icons.Default.DeleteSweep, onClick = { viewModel.clearCache() })
            SettingsItemDesign("About", Icons.Default.Info, "Musx 1.01 v1.0.0")
        }
    }

    if (showSpeedDialog) {
        com.musx.a1.ui.screens.player.SpeedDialog(
            currentSpeed = 1.0f,
            onDismiss = { showSpeedDialog = false },
            onSpeedSelected = {
                // In a future version, this would update a global Datastore preference
                Toast.makeText(context, "Default speed set to ${it}x", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (showSleepDialog) {
        com.musx.a1.ui.screens.player.SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onTimerSelected = {
                if (it > 0) {
                    Toast.makeText(context, "Global sleep timer set for $it minutes", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SettingsItemDesign(
    title: String,
    icon: ImageVector,
    value: String? = null,
    isSwitch: Boolean = false,
    switchChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (isSwitch) {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = SecondaryBlue)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        }
    }
}
