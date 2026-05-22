package com.fitcoachai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fitcoachai.app.data.model.ExerciseResult
import com.fitcoachai.app.data.model.RunSession
import com.fitcoachai.app.data.model.WorkoutExercise
import com.fitcoachai.app.viewmodel.ChatViewModel
import com.fitcoachai.app.viewmodel.ExerciseState
import com.fitcoachai.app.viewmodel.ExerciseViewModel
import com.fitcoachai.app.viewmodel.WorkoutViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onLogout: () -> Unit = {},
    onExerciseClick: (ExerciseResult) -> Unit = {},
    onShowSessions: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val chatViewModel: ChatViewModel = viewModel()

    Scaffold(
        bottomBar = {
            com.fitcoachai.app.ui.components.EliteNavigationBar(
                items = listOf(
                    com.fitcoachai.app.ui.components.EliteNavAction("COACH", Icons.Default.FitnessCenter),
                    com.fitcoachai.app.ui.components.EliteNavAction("RUN", Icons.Default.DirectionsRun),
                    com.fitcoachai.app.ui.components.EliteNavAction("AI", Icons.Default.AutoAwesome),
                    com.fitcoachai.app.ui.components.EliteNavAction("ME", Icons.Default.Person)
                ),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize().drawBehind {
            drawRect(Color(0xFF010208))
            val step = 40.dp.toPx()
            for (i in 0..(size.height / step).toInt()) {
                drawLine(Color.White.copy(0.015f), Offset(0f, i * step), Offset(size.width, i * step), 0.5.dp.toPx())
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(120),
                label = "hs_transition"
            ) { tab ->
                when (tab) {
                    0 -> DashboardTab(onExerciseClick = onExerciseClick) { focus ->
                        chatViewModel.askInspiration(focus)
                        selectedTab = 2
                    }
                    1 -> GPSTrackerScreen(onShowHistory = onShowSessions)
                    2 -> ChatbotScreen()
                    3 -> ProfileTab(onLogout = onLogout, onShowSessions = onShowSessions)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    onExerciseClick: (ExerciseResult) -> Unit,
    onTriggerInspiration: (String) -> Unit
) {
    val workoutViewModel: WorkoutViewModel = viewModel()
    val exerciseViewModel: ExerciseViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    
    val program by workoutViewModel.program.collectAsState()
    val totalCalories by workoutViewModel.totalCalories.collectAsState()
    val aiMessages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    
    val muscleList by exerciseViewModel.muscleExercises.collectAsState()
    val burnList by exerciseViewModel.burnExercises.collectAsState()
    val powerList by exerciseViewModel.powerExercises.collectAsState()
    val flowList by exerciseViewModel.flowExercises.collectAsState()

    var userName by remember { mutableStateOf("ATHLETE") }
    var userObjectif by remember { mutableStateOf("") }
    
    val dailyFocus = remember(aiMessages) {
        aiMessages.findLast { !it.isUser && it.content.length < 150 }?.content 
            ?: "SYNCHRONISATION..."
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("nom")?.uppercase() ?: "ATHLETE"
                    userObjectif = doc.getString("objectif") ?: ""
                    exerciseViewModel.loadExercisesByObjectif(userObjectif)
                    chatViewModel.generateNeuralFocus(userObjectif)
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header") { DashboardHeader(userName) }
        
        item(key = "stats") {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "CHARGE", "${program.size} EX", Color(0xFF00FFCC))
                StatCard(Modifier.weight(1f), "BRÛLÉ", "$totalCalories KCAL", Color(0xFFFF3D00))
            }
        }

        item(key = "focus") { DailyFocusShard(focus = dailyFocus, isLoading = isLoading, onRefresh = { chatViewModel.generateNeuralFocus(userObjectif) }) }

        val primaryPod = when {
            userObjectif.contains("poids", true) || userObjectif.contains("perdre", true) -> 
                GoalPodConfig("POUR VOUS: BRÛLER", "TARGET_BURN", Color(0xFFFF3D00), Icons.Default.Whatshot, burnList)
            userObjectif.contains("muscle", true) || userObjectif.contains("masse", true) ->
                GoalPodConfig("POUR VOUS: MASSE", "TARGET_MUSCLE", Color(0xFFFFB300), Icons.Default.FitnessCenter, muscleList)
            userObjectif.contains("force", true) || userObjectif.contains("puissance", true) ->
                GoalPodConfig("POUR VOUS: FORCE", "TARGET_POWER", Color(0xFF00FFCC), Icons.Default.Bolt, powerList)
            else -> null
        }

        primaryPod?.let { config ->
            item {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), color = config.color.copy(0.05f), border = BorderStroke(1.2.dp, config.color.copy(0.2f))) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        GoalPod(config.title, config.code, config.color, config.icon, config.exercises, onExerciseClick)
                    }
                }
            }
        }

        item { GoalPod("PRISE DE MASSE", "MUSCLE", Color(0xFFFFB300), Icons.Default.FitnessCenter, muscleList, onExerciseClick) }
        item { GoalPod("PERTE DE POIDS", "BURN", Color(0xFFFF3D00), Icons.Default.Whatshot, burnList, onExerciseClick) }
        item { GoalPod("FORCE ÉLITE", "POWER", Color(0xFF00FFCC), Icons.Default.Bolt, powerList, onExerciseClick) }
        item { GoalPod("ENDURANCE", "FLOW", Color(0xFF7C4DFF), Icons.Default.DirectionsRun, flowList, onExerciseClick) }
    }
}

data class GoalPodConfig(val title: String, val code: String, val color: Color, val icon: ImageVector, val exercises: List<ExerciseResult>)

@Composable
fun GoalPod(title: String, code: String, color: Color, icon: ImageVector, exercises: List<ExerciseResult>, onExerciseClick: (ExerciseResult) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Text("[$code]", color = color.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        if (exercises.isEmpty()) { Box(Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 16.dp).background(Color.White.copy(0.02f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = color, strokeWidth = 1.dp) } }
        else { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(exercises, key = { it.id }) { ex -> ExerciseShard(ex, color) { onExerciseClick(ex) } } } }
    }
}

@Composable
fun ExerciseShard(exercise: ExerciseResult, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(140.dp, 160.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A0F1E),
        border = BorderStroke(1.dp, accent.copy(0.15f))
    ) {
        Box {
            AsyncImage(model = exercise.imageUrl ?: "", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.5f })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
            Text(exercise.name.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 2, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
        }
    }
}

@Composable
fun DashboardHeader(name: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)) {
        Text("FITCOACH AI", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text(text = "BIENVENUE, $name", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Surface(modifier = modifier.height(70.dp), shape = RoundedCornerShape(12.dp), color = Color.White.copy(0.03f), border = BorderStroke(1.dp, Color.White.copy(0.06f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.White.copy(0.3f), fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DailyFocusShard(focus: String, isLoading: Boolean, onRefresh: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(80.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFF00FFCC).copy(0.04f), border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(0.15f))) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp).background(Color(0xFF00FFCC).copy(0.1f), CircleShape)) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("FOCUS DU JOUR", color = Color(0xFF00FFCC).copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Black); if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color(0xFF00FFCC), trackColor = Color.White.copy(0.03f)) else Text(focus, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2) }
        }
    }
}

@Composable
fun ProfileTab(onLogout: () -> Unit, onShowSessions: () -> Unit) {
    val chatViewModel: ChatViewModel = viewModel()
    val aiActivated by chatViewModel.isAiActivated.collectAsState()
    
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var userName by remember { mutableStateOf("UTILISATEUR") }
    var userObjectif by remember { mutableStateOf("Non défini") }
    var userAge by remember { mutableStateOf("-") }
    var userPoids by remember { mutableStateOf("-") }
    var recentSessions by remember { mutableStateOf<List<RunSession>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userName = doc.getString("nom")?.uppercase() ?: "UTILISATEUR"; userObjectif = doc.getString("objectif") ?: "Non défini"; userAge = doc.getString("age") ?: "-"; userPoids = doc.getString("poids") ?: "-"
            }
            db.collection("users").document(uid).collection("sessions").orderBy("date", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { docs -> recentSessions = docs.toObjects(RunSession::class.java) }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(160.dp)) {
                    val sw = 8.dp.toPx()
                    drawArc(color = Color.White.copy(0.05f), startAngle = 140f, sweepAngle = 260f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(listOf(Color(0xFF00D4FF), Color(0xFF7C4DFF), Color(0xFF00D4FF))), startAngle = 140f, sweepAngle = 180f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(70.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF7C4DFF)))), contentAlignment = Alignment.Center) { Text(userName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White) }
                    Spacer(Modifier.height(10.dp)); Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        item { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetabolicShard(Modifier.weight(1f), "ÂGE", userAge, "ANS", Icons.Default.History); MetabolicShard(Modifier.weight(1f), "POIDS", userPoids, "KG", Icons.Default.Scale) } }
        item { Column(Modifier.padding(horizontal = 16.dp)) { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.03f), border = BorderStroke(1.dp, Color.White.copy(0.06f))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color(0xFF00D4FF).copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Adjust, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp)) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("OBJECTIF ACTIF", color = Color.White.copy(0.3f), fontSize = 8.sp, fontWeight = FontWeight.Black); Text(userObjectif.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }; Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(0.1f), modifier = Modifier.size(12.dp)) } } } }
        
        // 🕹️ V16: AI ACTIVATION TOGGLE IN PROFILE
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.03f), border = BorderStroke(1.dp, Color.White.copy(0.06f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(if (aiActivated) Color(0xFF00FFCC).copy(0.1f) else Color.White.copy(0.05f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, tint = if (aiActivated) Color(0xFF00FFCC) else Color.White.copy(0.3f), modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("NOYAU INTELLIGENT", color = Color.White.copy(0.3f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text(if (aiActivated) "ACTIVÉ" else "DÉSACTIVÉ", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Switch(
                            checked = aiActivated,
                            onCheckedChange = { chatViewModel.toggleAiCore(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFCC), checkedTrackColor = Color(0xFF00FFCC).copy(0.3f))
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FLUX DE PERFORMANCE", color = Color.White.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("VOIR TOUT", color = Color(0xFF00D4FF), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onShowSessions() })
                }
                Spacer(Modifier.height(12.dp))
                recentSessions.forEach { session -> WorkoutSessionCard(session); Spacer(Modifier.height(8.dp)) }
            }
        }
        item { Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.03f)).border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(24.dp))) { ProfileActionRow(Icons.Default.Settings, "Paramètres"); HorizontalDivider(color = Color.White.copy(0.04f)); ProfileActionRow(Icons.Default.Security, "Sécurité"); HorizontalDivider(color = Color.White.copy(0.04f)); ProfileActionRow(Icons.AutoMirrored.Filled.Help, "Support") } }
        item { Box(Modifier.padding(horizontal = 16.dp)) { Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(0.3f)), contentPadding = PaddingValues(0.dp)) { Row(modifier = Modifier.fillMaxSize().background(Color(0xFFFF3B30).copy(0.05f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("TERMINER LA SESSION", color = Color(0xFFFF3B30), fontWeight = FontWeight.Black, fontSize = 12.sp) } } } }
    }
}

@Composable
fun ProfileActionRow(icon: ImageVector, label: String, onClick: () -> Unit = {}, trailingIcon: ImageVector = Icons.Default.ChevronRight) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)); Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(trailingIcon, null, tint = Color.White.copy(0.1f), modifier = Modifier.size(16.dp)) }
}

@Composable
fun MetabolicShard(modifier: Modifier, label: String, value: String, unit: String, icon: ImageVector) {
    Surface(modifier = modifier.height(110.dp), shape = RoundedCornerShape(24.dp), color = Color.White.copy(0.03f), border = BorderStroke(1.dp, Color.White.copy(0.06f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(16.dp))
            Column { Text(label, color = Color.White.copy(0.3f), fontSize = 8.sp, fontWeight = FontWeight.Black); Row(verticalAlignment = Alignment.Bottom) { Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(4.dp)); Text(unit, color = Color(0xFF00D4FF), fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun WorkoutSessionCard(session: RunSession) {
    val date = SimpleDateFormat("dd.MM.yy", Locale.FRENCH).format(Date(session.date))
    Row(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.03f)).border(0.5.dp, Color.White.copy(0.05f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF00D4FF).copy(0.08f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("SÉANCE", color = Color.White.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black); Text(date, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
        Text("%.2f KM".format(session.distanceMeters / 1000f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActiveSportBackground() {
    Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF01040A)); val step = 80.dp.toPx(); for (i in 0..(size.width / step).toInt()) { drawLine(color = Color.White.copy(0.012f), start = Offset(i * step, 0f), end = Offset(i * step - 250f, size.height), strokeWidth = 0.5.dp.toPx()) } }
}
