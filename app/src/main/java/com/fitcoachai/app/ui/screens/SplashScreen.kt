package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitcoachai.app.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: (isLoggedIn: Boolean) -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // 🔄 Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "elite_engine")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(3500, delayMillis = 500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    val neuralPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "neuralPulseAlpha"
    )
    
    val neuralPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "neuralPulseScale"
    )

    // 🚀 Entrance Animations
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(2500, easing = EaseInOutQuart),
        label = "alpha"
    )
    
    val bgScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.05f else 1.15f,
        animationSpec = tween(5000, easing = LinearOutSlowInEasing),
        label = "bgScale"
    )

    val scanProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else -0.2f,
        animationSpec = tween(4000, easing = EaseInOutCubic),
        label = "scan"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(4500)
        val currentUser = FirebaseAuth.getInstance().currentUser
        onSplashFinished(currentUser != null)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF020814)),
        contentAlignment = Alignment.Center
    ) {
        // 🏗️ BACKGROUND LAYER (ENFORCED MATHEMATICAL CENTERING)
        Image(
            painter = painterResource(id = R.drawable.background_splasher1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alphaAnim * 0.8f)
                .scale(bgScale), // Pure scale around Center alignment
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // Depth Gradient & Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF020814).copy(alpha = 0.9f)),
                        radius = 2800f
                    )
                )
        )

        // 🧬 NEURAL SCANNER
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = alphaAnim * 0.4f)) {
            val y = size.height * scanProgress
            if (y in -50f..size.height + 50f) {
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF00D4FF), Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // 💎 CENTER CONTENT COLUMN
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = alphaAnim)
        ) {
            // LOGO WITH NEURAL PULSE
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer(scaleX = neuralPulseScale, scaleY = neuralPulseScale, alpha = neuralPulseAlpha)
                        .border(1.dp, Color(0xFF00D4FF).copy(0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFF00D4FF).copy(0.15f), CircleShape)
                        .blur(40.dp)
                )
                Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.logoo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.7f)
                            .graphicsLayer(translationX = shimmerTranslate, rotationZ = 45f)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(0.25f), Color.Transparent)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val letterSpacing by animateFloatAsState(
                targetValue = if (startAnimation) 14f else 4f,
                animationSpec = tween(3500, easing = EaseOutQuart),
                label = "spacing"
            )

            Text(
                text = "FITCOACH AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing.sp
            )

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(2.dp)
                    .width(70.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF00D4FF), Color.Transparent)
                        )
                    )
            )

            Text(
                text = "ULTIMATE PERFORMANCE ENGINE",
                color = Color(0xFF00D4FF).copy(0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 22.dp)
            )
        }

        // 🔋 LOADING SYSTEM
        val progress by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(4300, easing = LinearEasing),
            label = "progress"
        )
        
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("INITIALIZING SYSTEM...", color = Color.White.copy(0.3f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.height(2.dp).fillMaxWidth(0.4f).background(Color.White.copy(0.05f), RoundedCornerShape(1.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color(0xFF00D4FF), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}
