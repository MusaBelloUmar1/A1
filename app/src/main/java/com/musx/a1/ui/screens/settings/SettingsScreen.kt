package com.musx.a1.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musx.a1.ui.components.NeumorphicCard
import com.musx.a1.ui.theme.PrimaryBlue

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingsGroup(title = "Playback") {
            SettingsItem("Playback Speed")
            SettingsItem("Sleep Timer")
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup(title = "Content") {
            SettingsItem("Manage Folders")
            SettingsItem("Clear Cache")
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        // Add arrows or toggles here
    }
}
