package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitcoachai.app.R
import com.fitcoachai.app.viewmodel.AuthState
import com.fitcoachai.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var age by remember { mutableStateOf("") }
    var poids by remember { mutableStateOf("") }
    var objectif by remember { mutableStateOf("Perdre du poids") }
    var expanded by remember { mutableStateOf(false) }
    var navigationDone by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val objectifs = listOf("Perdre du poids", "Gagner du muscle", "Endurance", "Rester en forme")

    LaunchedEffect(authState) {
        if (authState is AuthState.Success && !navigationDone) {
            navigationDone = true
            delay(1500)
            authViewModel.resetState()
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF01040A))
    ) {
        AuthCyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 💎 MINI LOGO SHARD
            Image(
                painter = painterResource(id = R.drawable.logoo),
                contentDescription = "FitCoach AI Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NEURAL_REGISTRATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00D4FF),
                letterSpacing = 3.sp
            )

            Text(
                text = "Créer un Profil Élite",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🧬 REGISTRATION CLUSTER
            CyberAuthTextField(
                value = nom,
                onValueChange = { nom = it },
                label = "NOM D'OPÉRATEUR",
                icon = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            CyberAuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "REAIS DE COMMUNICATION (EMAIL)",
                icon = Icons.Outlined.AlternateEmail,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            CyberAuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "CODE D'ACCÈS CRYTÉ",
                icon = Icons.Outlined.VpnKey,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CyberAuthTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = "CYCLE DE VIE (ÂGE)",
                    icon = Icons.Outlined.History,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )

                CyberAuthTextField(
                    value = poids,
                    onValueChange = { poids = it },
                    label = "MASSE (KG)",
                    icon = Icons.Outlined.MonitorWeight,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🎯 OBJECTIVE SELECTION (Premium Styled)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "OBJECTIF DE MISSION",
                    color = Color.White.copy(0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.White.copy(0.03f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.08f)),
                        modifier = Modifier.fillMaxWidth().height(60.dp).menuAnchor()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.TrackChanges, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = objectif.uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF0A121E))
                    ) {
                        objectifs.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) },
                                onClick = {
                                    objectif = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = authState is AuthState.Error) {
                val error = (authState as? AuthState.Error)?.message ?: ""
                Text(error.uppercase(), color = Color(0xFFFF3B30), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))

            CyberAuthButton(
                text = "GÉNÉRER LE PROFIL ÉLITE",
                isLoading = authState is AuthState.Loading
            ) {
                authViewModel.register(nom, email, password, age, poids, objectif)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DÉJÀ OPÉRATEUR ? ", color = Color.White.copy(0.3f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                TextButton(onClick = onNavigateToLogin) {
                    Text("SE CONNECTER", color = Color(0xFF00D4FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
