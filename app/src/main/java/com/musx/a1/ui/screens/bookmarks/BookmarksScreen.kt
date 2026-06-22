package com.musx.a1.ui.screens.bookmarks

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
import com.musx.a1.data.entity.Bookmark
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.theme.PrimaryBlue

@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Bookmarks",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(bookmarks) { bookmark ->
                BookmarkItem(bookmark)
            }
        }
    }
}

@Composable
fun BookmarkItem(bookmark: Bookmark) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = bookmark.snippet, maxLines = 2, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chapter ${bookmark.chapterIndex + 1}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}
