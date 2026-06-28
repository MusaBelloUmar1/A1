package com.musx.a1.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.data.entity.Book
import com.musx.a1.ui.components.EmptyState
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.components.shimmer.ShimmerItem
import com.musx.a1.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onBookClick: (Book) -> Unit
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val appState by viewModel.appState.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Songs", "Artists", "Albums", "Folders")

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            Column(modifier = Modifier.background(BackgroundWhite)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                    Text(
                        text = "All Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Row {
                        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue) }
                        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = PrimaryBlue) }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryBlue,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryBlue
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (allBooks.isEmpty()) {
            if (appState is com.musx.a1.ui.state.AppState.LoadingLibrary) {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    repeat(5) {
                        ShimmerItem(modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 8.dp))
                    }
                }
            } else {
                EmptyState(
                    icon = Icons.Default.Book,
                    title = "No books found",
                    description = "Scan a folder to add your PDF books to the library.",
                    actionText = "Go to Settings",
                    onAction = onSettingsClick
                )
            }
        } else {
            val filteredBooks = when(selectedTab) {
                1 -> allBooks.filter { it.favorite } // Simple artist/fav mix for now
                else -> allBooks
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBooks) { book ->
                    BookListItem(book = book, onClick = { onBookClick(book) })
                }
            }
        }
    }
}

@Composable
fun BookListItem(book: Book, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicCard(
            modifier = Modifier.size(56.dp),
            cornerRadius = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Book, contentDescription = null, tint = PrimaryBlue)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
            Text(text = "${book.totalPages} pages", fontSize = 12.sp, color = Color.Gray)
        }
        Icon(
            Icons.Default.MoreVert,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}
