package com.musx.a1.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musx.a1.data.entity.Book
import com.musx.a1.ui.components.EmptyState
import com.musx.a1.ui.components.shimmer.ShimmerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onBookClick: (Book) -> Unit) {
    val allBooks by viewModel.allBooks.collectAsState()
    val recentlyOpened by viewModel.recentlyOpened.collectAsState()
    val appState by viewModel.appState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                }
            )
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
                    onAction = {}
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (recentlyOpened.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently Opened",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(recentlyOpened) { book ->
                        BookCard(book = book, onClick = { onBookClick(book) })
                    }
                }

                item {
                    Text(
                        text = "All Books",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(allBooks) { book ->
                    BookCard(book = book, onClick = { onBookClick(book) })
                }
            }
        }
    }
}

@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${book.totalPages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
