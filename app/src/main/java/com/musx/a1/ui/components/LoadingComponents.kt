package com.musx.a1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.state.AppState

@Composable
fun FullScreenLoading(state: AppState) {
    if (state is AppState.Idle || state is AppState.Ready) return

    val message = when (state) {
        is AppState.LoadingLibrary -> "Loading library..."
        is AppState.ScanningFolders -> "Scanning folders..."
        is AppState.ParsingPdf -> "Parsing PDF..."
        is AppState.PreparingNarration -> "Preparing narration..."
        is AppState.LoadingBook -> "Loading book..."
        is AppState.Error -> "Error: ${state.message}"
        else -> "Working..."
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state is AppState.Error) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            } else if (state !is AppState.Idle && state !is AppState.Ready) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
