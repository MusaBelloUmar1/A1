package com.musx.a1.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val book by viewModel.currentBook.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sentence by viewModel.currentSentence.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val shuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val sleepTimer by viewModel.sleepTimerMillis.collectAsState()

    var selectedTab by remember { mutableStateOf(1) } // 0: Chapters, 1: Now Playing, 2: Transcript
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF647DEE), Color(0xFF7F53AC))
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradientBackground),
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(book?.title ?: "Now Playing", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Musx A1", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerTabItem("Chapters", selectedTab == 0) { selectedTab = 0 }
                PlayerTabItem("Playback", selectedTab == 1) { selectedTab = 1 }
                PlayerTabItem("Transcript", selectedTab == 2) { selectedTab = 2 }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> ChaptersTab(viewModel)
                1 -> NowPlayingContent(
                    book,
                    sentence,
                    currentPageIndex,
                    isPlaying,
                    speed,
                    shuffleEnabled,
                    repeatMode,
                    sleepTimer,
                    onSpeedClick = { showSpeedDialog = true },
                    onSleepClick = { showSleepDialog = true },
                    viewModel = viewModel
                )
                2 -> TranscriptTab(sentence)
            }
        }
    }

    if (showSpeedDialog) {
        SpeedDialog(currentSpeed = speed, onDismiss = { showSpeedDialog = false }, onSpeedSelected = { viewModel.setPlaybackSpeed(it) })
    }
    if (showSleepDialog) {
        SleepTimerDialog(onDismiss = { showSleepDialog = false }, onTimerSelected = { viewModel.setSleepTimer(it) })
    }
}

@Composable
fun PlayerTabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SpeedDialog(currentSpeed: Float, onDismiss: () -> Unit, onSpeedSelected: (Float) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", color = PrimaryBlue) },
        text = {
            Column {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSpeedSelected(speed)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSpeed == speed, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${speed}x", color = PrimaryBlue)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = PrimaryBlue) }
        },
        containerColor = BackgroundWhite
    )
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onTimerSelected: (Int) -> Unit) {
    var selectedMinutes by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Sleep Timer",
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Timer UI (Simplified representation of Screen 11)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.05f))
                        .border(4.dp, PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { selectedMinutes / 120f },
                        modifier = Modifier.size(140.dp),
                        color = AccentYellow,
                        strokeWidth = 8.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$selectedMinutes",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Minutes",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Quick Select", color = PrimaryBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(10, 20, 30, 60, 90).forEach { mins ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMinutes == mins) AccentYellow else PrimaryBlue.copy(alpha = 0.05f))
                                .clickable { selectedMinutes = mins }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 12.sp,
                                color = if (selectedMinutes == mins) PrimaryBlue else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimerSelected(selectedMinutes)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Timer", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onTimerSelected(0)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Off", color = Color.Gray)
            }
        },
        containerColor = BackgroundWhite
    )
}

@Composable
fun NowPlayingContent(
    book: com.musx.a1.data.entity.Book?,
    sentence: String,
    currentPageIndex: Int,
    isPlaying: Boolean,
    speed: Float,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimer: Long,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    viewModel: PlayerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Progress/Artwork
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(8.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = Color(0xFF7F53AC)
                )
            }
            // Simple Progress Indicator
            CircularProgressIndicator(
                progress = { 0.35f },
                modifier = Modifier.size(240.dp),
                color = AccentYellow,
                strokeWidth = 8.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sentence,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Actions Row (Heart, Download, Share)
        val context = androidx.compose.ui.platform.LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    if (book?.favorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (book?.favorite == true) AccentYellow else Color.White
                )
            }
            IconButton(onClick = { Toast.makeText(context, "Download not available in preview", Toast.LENGTH_SHORT).show() }) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
            }
            IconButton(onClick = { Toast.makeText(context, "Sharing not available in preview", Toast.LENGTH_SHORT).show() }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time and Slider
        val totalPages = book?.totalPages ?: 1
        val progress = if (totalPages > 0) (currentPageIndex.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f

        Column {
            Slider(
                value = progress,
                onValueChange = { viewModel.seekTo(it) },
                colors = SliderDefaults.colors(
                    thumbColor = AccentYellow,
                    activeTrackColor = AccentYellow,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Page ${currentPageIndex + 1}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("$totalPages Pages", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        // Extra Controls (Repeat, Shuffle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                Icon(
                    when(repeatMode) {
                        1 -> Icons.Default.RepeatOne
                        2 -> Icons.Default.Repeat
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) AccentYellow else Color.White
                )
            }
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) AccentYellow else Color.White
                )
            }
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.skipPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            FloatingActionButton(
                onClick = { viewModel.togglePlayback() },
                containerColor = Color.White,
                contentColor = Color(0xFF7F53AC),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = { viewModel.skipNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        // Bottom Utils (Speed, Sleep)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PlayerUtilButton(Icons.Default.Timer, if (sleepTimer > 0) "${sleepTimer / 60000}m" else "Sleep Timer", onClick = onSleepClick)
            PlayerUtilButton(Icons.Default.Speed, "${speed}x", onClick = onSpeedClick)
        }
    }
}

@Composable
fun PlayerUtilButton(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun ChaptersTab(viewModel: PlayerViewModel) {
    val chapters by viewModel.chapters.collectAsState()
    if (chapters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chapters detected", color = Color.White.copy(alpha = 0.7f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${chapter.title}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "02:58", // Placeholder
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun TranscriptTab(sentence: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text = sentence,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}
