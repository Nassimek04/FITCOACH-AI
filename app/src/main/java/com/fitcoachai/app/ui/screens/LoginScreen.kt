package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitcoachai.app.R
import com.fitcoachai.app.viewmodel.AuthState
import com.fitcoachai.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                authViewModel.resetState()
                onLoginSuccess()
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF01040A))
    ) {
        // 🌌 DYNAMIC NEURAL BACKGROUND
        AuthCyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 💎 PREMIUM LOGO SHARD
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00D4FF).copy(0.15f), Color.Transparent),
                                radius = size.width * 0.8f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logoo),
                    contentDescription = "FitCoach AI",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ACCESS_PORTAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00D4FF),
                letterSpacing = 4.sp
            )

            Text(
                text = "Bienvenue dans l'Élite",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 🧬 BIOMETRIC INPUT CLUSTER
            CyberAuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = "IDENTIFIANT SÉCURISÉ (EMAIL)",
                icon = Icons.Outlined.AlternateEmail,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            CyberAuthTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = "CLEF D'ACCÈS (PASSWORD)",
                icon = Icons.Outlined.VpnKey,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )

            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFF3B30).copy(0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(0.3f))
                ) {
                    Text(
                        text = errorMessage.uppercase(),
                        color = Color(0xFFFF3B30),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // 🚀 STEALTH COMMAND BUTTON
            CyberAuthButton(
                text = "INITIALISER LA CONNEXION",
                isLoading = authState is AuthState.Loading
            ) {
                authViewModel.login(email, password)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOUVELLE RECRUE ?",
                    color = Color.White.copy(0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "CRÉER UN PROFIL",
                        color = Color(0xFF00D4FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AuthCyberBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF01040A))
        
        // Shifting Nebula
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00D4FF).copy(0.08f), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.2f + (time * 100f)),
                radius = size.width * 1.2f
            )
        )
        
        // Precision Grid
        val step = 40.dp.toPx()
        for (x in 0..(size.width / step).toInt()) {
            drawLine(Color.White.copy(0.02f), Offset(x * step, 0f), Offset(x * step, size.height), 0.5.dp.toPx())
        }
        for (y in 0..(size.height / step).toInt()) {
            drawLine(Color.White.copy(0.02f), Offset(0f, y * step), Offset(size.width, y * step), 0.5.dp.toPx())
        }
    }
}

@Composable
fun CyberAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.White.copy(0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            color = Color.White.copy(0.03f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.08f)),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("............", color = Color.White.copy(0.1f), fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Color(0xFF00D4FF)),
                        singleLine = true,
                        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                    )
                }
                if (isPassword) {
                    IconButton(onClick = onPasswordToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberAuthButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Button(
        onClick = { 
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick() 
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF00D4FF).copy(0.5f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF)),
        shape = RoundedCornerShape(24.dp),
        enabled = !isLoading,
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 1.5.sp,
                fontSize = 14.sp
            )
        }
    }
}
