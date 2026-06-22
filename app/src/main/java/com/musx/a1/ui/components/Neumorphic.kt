package com.musx.a1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musx.a1.ui.theme.*

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = BackgroundWhite,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val shadowColor = ShadowDark.copy(alpha = 0.5f).toArgb()
                val lightColor = ShadowLight.copy(alpha = 0.9f).toArgb()

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint()
                    // Dark shadow (Bottom Right)
                    paint.color = backgroundColor.toArgb()
                    paint.setShadowLayer(8.dp.toPx(), 4.dp.toPx(), 4.dp.toPx(), shadowColor)
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerRadius.toPx(), cornerRadius.toPx(), paint)

                    // Light shadow (Top Left)
                    paint.setShadowLayer(8.dp.toPx(), (-4).dp.toPx(), (-4).dp.toPx(), lightColor)
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerRadius.toPx(), cornerRadius.toPx(), paint)
                }
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
    ) {
        content()
    }
}

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BackgroundWhite,
    textColor: Color = PrimaryBlue
) {
    NeumorphicCard(
        modifier = modifier.clickable { onClick() },
        backgroundColor = backgroundColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = textColor)
        }
    }
}
