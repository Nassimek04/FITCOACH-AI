package com.fitcoachai.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitcoachai.app.data.model.ChatMessage
import com.fitcoachai.app.data.model.ChatSession
import com.fitcoachai.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChatbotScreen() {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel()
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val aiOnline by chatViewModel.isAiOnline.collectAsState()
    val aiActivated by chatViewModel.isAiActivated.collectAsState()
    val suggestions by chatViewModel.suggestions.collectAsState()
    val sessions by chatViewModel.sessions.collectAsState()
    val currentSession by chatViewModel.currentSession.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var micLanguage by remember { mutableStateOf("fr-FR") } 
    
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var showDeleteConfirm by remember { mutableStateOf<ChatSession?>(null) }

    val themeColor by animateColorAsState(
        targetValue = when {
            !aiActivated -> Color(0xFF4A5568) // Deactivated Grey
            isLoading -> Color(0xFFFFCC00) 
            aiOnline -> Color(0xFF00F2FF)
            else -> Color(0xFFFF3D00)
        },
        animationSpec = tween(1000), label = ""
    )

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { chatViewModel.handleFileAttachment(it, context) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            chatViewModel.sendMessage("Analyse cette photo.", it) 
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) { listState.animateScrollToItem(messages.size - 1) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF01040A), drawerTonalElevation = 0.dp) {
                FitCoachArchiveDrawer(
                    sessions = sessions,
                    currentSession = currentSession,
                    accent = themeColor,
                    onSessionSelected = { session ->
                        chatViewModel.selectSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        chatViewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { session ->
                        showDeleteConfirm = session
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().drawBehind {
                drawRect(Color(0xFF010206))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(themeColor.copy(0.04f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.2f),
                        radius = size.width
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                    radius = size.width
                )
            },
            containerColor = Color.Transparent,
            topBar = {
                ChatHeader(
                    isOnline = aiOnline,
                    isActivated = aiActivated,
                    isTyping = isLoading,
                    themeColor = themeColor,
                    sessionTitle = currentSession?.title ?: "Nouvelle Discussion",
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onDeleteClick = { currentSession?.let { showDeleteConfirm = it } },
                    onToggleAi = { chatViewModel.toggleAiCore(!aiActivated) }
                )
            },
            bottomBar = {
                ChatDock(
                    inputText = inputText,
                    onTextChange = { inputText = it },
                    isLoading = isLoading,
                    isListening = isListening,
                    themeColor = themeColor,
                    currentLang = micLanguage,
                    onLangChange = { micLanguage = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            chatViewModel.sendMessage(inputText.trim(), language = micLanguage)
                            inputText = ""
                        }
                    },
                    onFileClick = { filePickerLauncher.launch("*/*") },
                    onCameraClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onMicClick = {
                        if (isListening) { 
                            speechRecognizer.stopListening()
                            isListening = false 
                        } else { 
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                startVoiceListening(speechRecognizer, micLanguage) { text -> 
                                    isListening = false
                                    if (text.isNotEmpty()) inputText = text 
                                }
                                isListening = true
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                ) {
                    items(messages, key = { it.timestamp }) { msg ->
                        ChatBubble(message = msg, accent = themeColor)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Supprimer la discussion ?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Voulez-vous vraiment supprimer cette conversation ?", color = Color.White.copy(0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { 
                        chatViewModel.deleteSession(it)
                        if (currentSession?.id == it.id) chatViewModel.createNewSession()
                    }
                    showDeleteConfirm = null
                }) { Text("SUPPRIMER", color = Color.Red, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("ANNULER", color = Color.White.copy(0.6f)) }
            },
            containerColor = Color(0xFF0A0F1E),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ChatHeader(isOnline: Boolean, isActivated: Boolean, isTyping: Boolean, themeColor: Color, sessionTitle: String, onMenuClick: () -> Unit, onDeleteClick: () -> Unit, onToggleAi: () -> Unit) {
    Surface(
        modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth().height(72.dp),
        color = Color.White.copy(0.03f),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, themeColor.copy(0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick, modifier = Modifier.background(Color.White.copy(0.05f), CircleShape)) { 
                Icon(Icons.Default.Menu, null, tint = themeColor) 
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sessionTitle.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulse by rememberInfiniteTransition().animateFloat(0.4f, 1f, infiniteRepeatable(tween(if (isTyping) 500 else 1500), RepeatMode.Reverse), label = "")
                    Box(Modifier.size(6.dp).graphicsLayer { alpha = if (isActivated) pulse else 1f }.background(if (!isActivated) Color.DarkGray else if (isOnline) Color(0xFF00FFCC) else Color.Red, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(if (!isActivated) "NOYAU DÉSACTIVÉ" else if (isTyping) "RÉFLEXION EN COURS..." else if (isOnline) "COACH EN LIGNE" else "HORS LIGNE", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            // 🕹️ Manual Toggle Button
            IconButton(onClick = onToggleAi) {
                Icon(if (isActivated) Icons.Default.PowerSettingsNew else Icons.Default.Power, null, tint = if (isActivated) Color(0xFF00FFCC) else Color.White.copy(0.3f))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.White.copy(0.3f))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, accent: Color) {
    val isUser = message.isUser
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isUser) accent.copy(0.1f) else Color.White.copy(0.04f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (isUser) 24.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 24.dp),
                border = BorderStroke(1.dp, if (isUser) accent.copy(0.2f) else Color.White.copy(0.06f)),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (message.image != null) {
                        Image(
                            bitmap = message.image.asImageBitmap(), 
                            contentDescription = null, 
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)), 
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    ChatMarkdownText(content = message.content, accentColor = accent)
                }
            }
            Text(text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)), color = Color.White.copy(0.2f), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp))
        }
    }
}

@Composable
fun ChatMarkdownText(content: String, accentColor: Color) {
    val annotatedString = remember(content, accentColor) {
        buildAnnotatedString {
            val text = content.replace(Regex("\\[.*?\\] "), "")
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var lastIndex = 0
            boldRegex.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = accentColor)) { append(match.groupValues[1]) }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    }
    Text(text = annotatedString, color = Color.White.copy(0.9f), fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
fun ChatDock(
    inputText: String, 
    onTextChange: (String) -> Unit, 
    isLoading: Boolean, 
    isListening: Boolean, 
    themeColor: Color, 
    currentLang: String,
    onLangChange: (String) -> Unit,
    onSend: () -> Unit, 
    onFileClick: () -> Unit, 
    onMicClick: () -> Unit, 
    onCameraClick: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("fr-FR" to "FR", "en-US" to "EN", "ar-SA" to "AR").forEach { (code, label) ->
                val active = currentLang == code
                Surface(
                    onClick = { onLangChange(code) },
                    shape = CircleShape,
                    color = if (active) themeColor.copy(0.15f) else Color.White.copy(0.03f),
                    border = BorderStroke(0.5.dp, if (active) themeColor else Color.White.copy(0.1f))
                ) {
                    Text(label, color = if (active) themeColor else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }

        Surface(modifier = Modifier.padding(16.dp).fillMaxWidth().height(68.dp), color = Color(0xFF080D1A), shape = RoundedCornerShape(34.dp), border = BorderStroke(1.dp, themeColor.copy(0.15f))) {
            Row(modifier = Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFileClick) { Icon(Icons.Default.Add, null, tint = themeColor.copy(0.7f)) }
                IconButton(onClick = onCameraClick) { Icon(Icons.Default.PhotoCamera, null, tint = themeColor.copy(0.7f)) }
                TextField(value = inputText, onValueChange = onTextChange, placeholder = { Text("Écrivez votre message...", color = Color.White.copy(0.2f), fontSize = 14.sp) }, modifier = Modifier.weight(1f), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White), singleLine = true)
                IconButton(onClick = onMicClick) { Icon(if (isListening) Icons.Default.Mic else Icons.Default.MicNone, null, tint = if (isListening) Color.Red else themeColor.copy(0.7f)) }
                FloatingActionButton(onClick = onSend, containerColor = if (inputText.isNotBlank()) themeColor else Color.White.copy(0.05f), contentColor = Color.Black, shape = CircleShape, modifier = Modifier.size(48.dp), elevation = FloatingActionButtonDefaults.elevation(0.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
fun FitCoachArchiveDrawer(sessions: List<ChatSession>, currentSession: ChatSession?, accent: Color, onSessionSelected: (ChatSession) -> Unit, onNewChat: () -> Unit, onDeleteSession: (ChatSession) -> Unit) {
    Column(modifier = Modifier.fillMaxHeight().padding(24.dp)) {
        Text("HISTORIQUE", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.padding(bottom = 32.dp))
        Button(onClick = onNewChat, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.Black); Spacer(Modifier.width(12.dp)); Text("NOUVELLE DISCUSSION", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        Spacer(Modifier.height(32.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sessions) { s ->
                val active = currentSession?.id == s.id
                Surface(
                    onClick = { onSessionSelected(s) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) accent.copy(0.1f) else Color.White.copy(0.02f),
                    border = if (active) BorderStroke(1.dp, accent.copy(0.3f)) else null
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, null, tint = if (active) accent else Color.White.copy(0.2f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(s.title, color = Color.White, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onDeleteSession(s) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.1f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun startVoiceListening(recognizer: SpeechRecognizer, langCode: String, onResult: (String) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
    }
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) { val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""; onResult(text) }
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) { onResult("") }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    recognizer.startListening(intent)
}
