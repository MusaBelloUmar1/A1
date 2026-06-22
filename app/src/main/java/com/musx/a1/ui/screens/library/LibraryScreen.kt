package com.musx.a1.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.data.entity.Book
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.theme.PrimaryBlue

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onBookClick: (Book) -> Unit) {
    val allBooks by viewModel.allBooks.collectAsState()
    val recentlyOpened by viewModel.recentlyOpened.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Library",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recentlyOpened.isNotEmpty()) {
                item {
                    Text("Recently Opened", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(recentlyOpened) { book ->
                    BookItem(book = book, onClick = { onBookClick(book) })
                }
            }

            item {
                Text("All Books", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(allBooks) { book ->
                BookItem(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = book.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Pages: ${book.totalPages}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}
