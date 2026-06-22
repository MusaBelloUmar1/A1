package com.musx.a1.ui.screens.importer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicButton
import com.musx.a1.ui.theme.PrimaryBlue

@Composable
fun ImportBottomSheet(
    fileName: String,
    onListenNow: () -> Unit,
    onAddToLibrary: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PDF Detected",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = fileName, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(24.dp))

        NeumorphicButton(
            text = "Listen Now",
            onClick = onListenNow,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        NeumorphicButton(
            text = "Add to Library",
            onClick = onAddToLibrary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
