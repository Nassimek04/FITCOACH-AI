package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fitcoachai.app.data.api.RetrofitClient
import com.fitcoachai.app.data.model.ExerciseDetailResponse
import com.fitcoachai.app.data.model.ExerciseResult
import com.fitcoachai.app.viewmodel.WorkoutViewModel

@Composable
fun ExerciseDetailScreen(
    exercise: ExerciseResult,
    onBack: () -> Unit,
    onAddedToProgram: () -> Unit = {}
) {
    var detail by remember { mutableStateOf<ExerciseDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val workoutViewModel: WorkoutViewModel = viewModel()
    val program by workoutViewModel.program.collectAsState()
    val addedMessage by workoutViewModel.addedMessage.collectAsState()
    var showSnackbar by remember { mutableStateOf(false) }

    val isAlreadyInProgram = program.any { it.id == exercise.id }

    LaunchedEffect(addedMessage) {
        if (addedMessage.isNotEmpty()) {
            showSnackbar = true
            kotlinx.coroutines.delay(1200)
            showSnackbar = false
            workoutViewModel.resetMessage()
            if (addedMessage.contains("✅")) onAddedToProgram()
        }
    }

    LaunchedEffect(exercise.id) {
        try {
            if (exercise.id < 100000) { // If it's from GitHub, try to get extra technical context from Wger
                detail = RetrofitClient.wgerApi.getExerciseDetail(exercise.id)
            }
        } catch (e: Exception) { }
        isLoading = false
    }

    val accentColor = when {
        exercise.category.contains("chest", true) -> Color(0xFFFFB300)
        exercise.category.contains("cardio", true) -> Color(0xFFFF3D00)
        exercise.category.contains("power", true) -> Color(0xFF00FFCC)
        else -> Color(0xFF00D4FF)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF01040A))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // --- HEADER MEDIA ---
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(
                    model = exercise.imageUrl ?: "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=500&auto=format",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF01040A)))))
                
                IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(16.dp).background(Color.Black.copy(0.4f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }

            // --- CONTENT POD ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(exercise.name.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 34.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(accentColor.copy(0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(exercise.category.uppercase(), color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("NIVEAU: ${exercise.level.uppercase()}", color = Color.White.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))

                // --- TACTICAL STATS ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailStatShard(Modifier.weight(1f), "MUSCLE", exercise.muscle, Icons.Default.FitnessCenter, accentColor)
                    DetailStatShard(Modifier.weight(1f), "MATÉRIEL", exercise.equipment ?: "Aucun", Icons.Default.Handyman, Color(0xFF7C4DFF))
                }

                Spacer(Modifier.height(32.dp))

                // --- INSTRUCTIONS ---
                Text("PROTOCOLE D'EXÉCUTION", color = Color.White.copy(0.3f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))
                
                if (exercise.instructions.isNotEmpty()) {
                    exercise.instructions.forEachIndexed { index, step ->
                        InstructionRow(index + 1, step)
                        Spacer(Modifier.height(12.dp))
                    }
                } else {
                    Text(exercise.description, color = Color.White.copy(0.7f), fontSize = 14.sp, lineHeight = 22.sp)
                }

                Spacer(Modifier.height(32.dp))

                // --- SAFETY SHARD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF3D00).copy(0.05f),
                    border = BorderStroke(1.dp, Color(0xFFFF3D00).copy(0.2f))
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Color(0xFFFF3D00), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("SÉCURITÉ ÉLITE", color = Color(0xFFFF3D00), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(exercise.safetyTips, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        // --- ACTION DOCK ---
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp)) {
            Button(
                onClick = {
                    if (!isAlreadyInProgram) {
                        workoutViewModel.addExercise(exercise.id, exercise.name, exercise.category, exercise.muscle, exercise.imageUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isAlreadyInProgram) Color.White.copy(0.1f) else accentColor),
                enabled = !isAlreadyInProgram
            ) {
                Icon(if (isAlreadyInProgram) Icons.Default.Check else Icons.Default.Add, null, tint = if (isAlreadyInProgram) Color.White.copy(0.4f) else Color.Black)
                Spacer(Modifier.width(12.dp))
                Text(if (isAlreadyInProgram) "DÉJÀ DANS LE PROGRAMME" else "AJOUTER À LA SESSION", color = if (isAlreadyInProgram) Color.White.copy(0.4f) else Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun InstructionRow(index: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = index.toString(), color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
        Text(text = text, color = Color.White.copy(0.8f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun DetailStatShard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.03f), border = BorderStroke(1.dp, Color.White.copy(0.06f))) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, color = Color.White.copy(0.3f), fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
