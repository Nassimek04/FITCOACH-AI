package com.fitcoachai.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbitOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-80).dp)
                .graphicsLayer(rotationZ = orbitOffset)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF00D4FF).copy(0.12f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 120.dp)
                .graphicsLayer(rotationZ = -orbitOffset)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF7C4DFF).copy(0.1f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
fun PremiumLiveBadge(text: String = "LIVE") {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF00C853).copy(alpha = 0.12f))
            .border(1.dp, Color(0xFF00C853).copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer(alpha = alpha)
                .background(Color(0xFF00C853), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            color = Color(0xFF00C853),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PremiumGlassBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = Color(0xFF0D1F35).copy(alpha = 0.85f),
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF00D4FF).copy(0.4f), Color.Transparent)
                    ),
                    topLeft = Offset(0f, size.height - 1f),
                    size = Size(size.width, 2f)
                )
            },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
