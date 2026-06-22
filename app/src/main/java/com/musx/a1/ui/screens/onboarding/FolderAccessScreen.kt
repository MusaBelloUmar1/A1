package com.musx.a1.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicButton

@Composable
fun FolderAccessScreen(onFolderSelected: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Folder Access",
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Select a folder where your PDF books are stored.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        NeumorphicButton(
            text = "Select Folder",
            onClick = onFolderSelected
        )
    }
}
