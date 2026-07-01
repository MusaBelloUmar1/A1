package com.musx.a1.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.screens.library.LibraryViewModel
import com.musx.a1.ui.screens.player.PlayerViewModel
import com.musx.a1.ui.screens.playlists.PlaylistsViewModel
import com.musx.a1.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistsViewModel: PlaylistsViewModel,
    onSettingsClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onRecentsClick: () -> Unit
) {
    val allBooks by libraryViewModel.allBooks.collectAsState()
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playlists by playlistsViewModel.playlists.collectAsState()

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryBlue)
                }
                Text(
                    text = "My Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                IconButton(onClick = onLibraryClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Large Circular Icon Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                NeumorphicCard(
                    modifier = Modifier.size(140.dp),
                    cornerRadius = 70.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Grid of Actions - Replaced with FlowRow or simple Rows for scrolling compatibility
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardActionCard("All Songs", "${allBooks.size} books", Icons.AutoMirrored.Filled.List, onLibraryClick)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val favCount = allBooks.count { it.favorite }
                        DashboardActionCard("Favorites", "$favCount books", Icons.Default.Favorite, onFavoritesClick)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardActionCard("Playlists", "${playlists.size} playlists", Icons.AutoMirrored.Filled.QueueMusic, onPlaylistsClick)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val recentCount = allBooks.count { it.lastOpenedAt > 0 }
                        DashboardActionCard("Recents", "$recentCount books", Icons.Default.History, onRecentsClick)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mini Player
            if (currentBook != null) {
                MiniPlayer(
                    title = currentBook?.title ?: "Unknown",
                    artist = "Musx Narration",
                    isPlaying = isPlaying,
                    onPlayPause = { playerViewModel.togglePlayback() },
                    onClick = onPlayerClick,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MiniPlayer(title: String, artist: String, isPlaying: Boolean, onPlayPause: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NeumorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Artwork
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryBlue)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue, maxLines = 1)
                Text(text = artist, fontSize = 12.sp, color = Color.Gray)
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
