package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitcoachai.app.data.model.RunSession
import com.fitcoachai.app.data.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

sealed class EliteSession {
    data class Run(val session: RunSession) : EliteSession()
    data class Workout(val session: WorkoutSession) : EliteSession()
    val date: Long get() = when(this) {
        is Run -> session.date
        is Workout -> session.date
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(onBack: () -> Unit) {
    var sessions by remember { mutableStateOf<List<EliteSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                val runDocs = db.collection("users").document(uid).collection("sessions").get().await()
                val workoutDocs = db.collection("users").document(uid).collection("workout_sessions").get().await()
                
                val runs = runDocs.toObjects(RunSession::class.java).map { EliteSession.Run(it) }
                val workouts = workoutDocs.toObjects(WorkoutSession::class.java).map { EliteSession.Workout(it) }
                
                sessions = (runs + workouts).sortedByDescending { it.date }
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        }
    }

    val totalKm = sessions.filterIsInstance<EliteSession.Run>().sumOf { it.session.distanceMeters.toDouble() } / 1000.0
    val totalVolume = sessions.filterIsInstance<EliteSession.Workout>().sumOf { it.session.totalVolumeKg }
    val totalCalories = sessions.sumOf { 
        when(it) {
            is EliteSession.Run -> it.session.calories
            is EliteSession.Workout -> it.session.totalCalories
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020814))) {
        PremiumHistoryBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("HISTORIQUE ÉLITE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp, color = Color.White)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF00D4FF)) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0D1F35).copy(alpha = 0.9f))
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00D4FF)) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { PremiumHistorySummary(totalKm, totalVolume, totalCalories, sessions.size) }

                    if (sessions.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1A2F4A))
                                    Spacer(Modifier.height(16.dp))
                                    Text("Aucune séance enregistrée", color = Color(0xFF556080))
                                }
                            }
                        }
                    } else {
                        items(sessions) { eliteSession ->
                            when(eliteSession) {
                                is EliteSession.Run -> PremiumRunCard(eliteSession.session)
                                is EliteSession.Workout -> PremiumWorkoutCard(eliteSession.session)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumHistoryBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbitOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.Center)
                .offset(x = 150.dp, y = (-200).dp)
                .graphicsLayer(rotationZ = orbitOffset)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF7C4DFF).copy(0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
fun PremiumHistorySummary(km: Double, volume: Double, cal: Int, count: Int) {
    Surface(
        color = Color(0xFF0D1F35).copy(alpha = 0.8f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4FF).copy(0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStatItem("Cardio", "%.1f".format(km), "KM", Color(0xFF00D4FF))
                SummaryStatItem("Force", "%.0f".format(volume), "KG", Color(0xFF00FFCC))
                SummaryStatItem("Énergie", "$cal", "KCAL", Color(0xFFFF3B30))
                SummaryStatItem("Total", "$count", "SESS.", Color(0xFF7C4DFF))
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(unit, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(label, color = Color(0xFF556080), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PremiumRunCard(session: RunSession) {
    PremiumSessionContainer(session.date, "COURSE PERFORMANCE", Color(0xFF00D4FF), Icons.Default.DirectionsRun) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat(Icons.Default.Route, "%.2f km".format(session.distanceMeters / 1000f), Color(0xFF00D4FF))
            MiniStat(Icons.Default.Timer, formatDuration(session.durationSeconds), Color(0xFF7C4DFF))
            MiniStat(Icons.Default.Whatshot, "${session.calories} cal", Color(0xFFFF3B30))
        }
    }
}

@Composable
fun PremiumWorkoutCard(session: WorkoutSession) {
    PremiumSessionContainer(session.date, "SESSION MUSCULATION", Color(0xFF00FFCC), Icons.Default.FitnessCenter) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat(Icons.Default.Scale, "%.0f kg".format(session.totalVolumeKg), Color(0xFF00FFCC))
            MiniStat(Icons.Default.Timer, formatDuration(session.durationSeconds), Color(0xFF7C4DFF))
            MiniStat(Icons.Default.Whatshot, "${session.totalCalories} cal", Color(0xFFFF3B30))
        }
    }
}

@Composable
fun PremiumSessionContainer(dateLong: Long, title: String, accent: Color, icon: ImageVector, content: @Composable () -> Unit) {
    val date = Date(dateLong)
    val dayStr = SimpleDateFormat("dd", Locale.FRENCH).format(date)
    val monthStr = SimpleDateFormat("MMM", Locale.FRENCH).format(date).uppercase()

    Surface(
        color = Color(0xFF0D1F35).copy(alpha = 0.7f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(50.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(0.1f)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(monthStr, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(title, color = accent.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(4.dp))
                content()
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF1A2F4A))
        }
    }
}


@Composable
fun MiniStat(icon: ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, color = Color.White.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
