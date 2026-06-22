package com.musx.a1.ui.screens.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicButton
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.theme.PrimaryBlue

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val book by viewModel.currentBook.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sentence by viewModel.currentSentence.collectAsState()

    var selectedTab by remember { mutableStateOf(1) } // 0: Chapters, 1: Now Playing, 2: Live Text

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chapters") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Now Playing") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Live Text") })
        }

        when (selectedTab) {
            0 -> ChaptersTab(viewModel)
            1 -> NowPlayingTab(book?.title ?: "Unknown", sentence, isPlaying) { viewModel.togglePlayback() }
            2 -> LiveTextTab(sentence)
        }
    }
}

@Composable
fun ChaptersTab(viewModel: PlayerViewModel) {
    // List of chapters
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Chapters List")
    }
}

@Composable
fun NowPlayingTab(title: String, sentence: String, isPlaying: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Large Cover Artwork Area
        NeumorphicCard(
            modifier = Modifier.size(280.dp).padding(16.dp),
            cornerRadius = 24.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📚", fontSize = 80.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = sentence, textAlign = TextAlign.Center, fontSize = 16.sp, minLines = 3)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Progress Slider Placeholder
            Slider(value = 0.3f, onValueChange = {}, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Text("⏮", fontSize = 24.sp) }
                NeumorphicButton(
                    text = if (isPlaying) "⏸" else "▶",
                    onClick = onToggle
                )
                IconButton(onClick = {}) { Text("⏭", fontSize = 24.sp) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {}) { Text("1.0x", color = com.musx.a1.ui.theme.AccentYellow) }
                TextButton(onClick = {}) { Text("⏱ Sleep") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LiveTextTab(sentence: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = sentence, fontSize = 22.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}
