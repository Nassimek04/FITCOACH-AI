package com.fitcoachai.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachai.app.data.model.ChatMessage
import com.fitcoachai.app.data.model.ChatSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ChatViewModel : ViewModel() {

    // Use 192.168.1.4 for physical devices on your network
    // Use 10.0.2.2 for the Android Emulator
    private val ollamaUrl = "http://10.148.190.57:11434/api/chat"
    private val ollamaTagsUrl = "http://10.148.190.57:11434/api/tags"
    private val defaultModel = "gemma3:12b" 

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAiOnline = MutableStateFlow(false)
    val isAiOnline: StateFlow<Boolean> = _isAiOnline.asStateFlow()

    private val _isAiActivated = MutableStateFlow(true)
    val isAiActivated: StateFlow<Boolean> = _isAiActivated.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(listOf(
        "Comment perdre du poids ? 📉",
        "Programme musculation débutant 💪",
        "Idées de repas protéinés 🥗"
    ))
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var sessionListener: ListenerRegistration? = null
    private var messageListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                startRealTimeSessionSync()
            } else {
                clearUserData()
            }
        }
        startConnectivityMonitor()
    }

    fun toggleAiCore(activated: Boolean) {
        _isAiActivated.value = activated
    }

    private fun clearUserData() {
        sessionListener?.remove()
        messageListener?.remove()
        _sessions.value = emptyList()
        _messages.value = emptyList()
        _currentSession.value = null
    }

    private fun startConnectivityMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _isAiOnline.value = checkOllamaStatus()
                delay(if (_isAiOnline.value) 10000 else 3000)
            }
        }
    }

    private suspend fun checkOllamaStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(ollamaTagsUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
            }
            connection.responseCode == 200
        } catch (e: Exception) { false }
    }

    private fun startRealTimeSessionSync() {
        val uid = auth.currentUser?.uid ?: return
        sessionListener?.remove()
        sessionListener = db.collection("users").document(uid).collection("chat_sessions").orderBy("lastTimestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            val sessionList = snapshot?.documents?.map { doc ->
                ChatSession(id = doc.id, title = doc.getString("title") ?: "Sequence", lastTimestamp = doc.getLong("lastTimestamp") ?: System.currentTimeMillis())
            } ?: emptyList()
            
            _sessions.value = sessionList
            if (_sessions.value.isNotEmpty() && _currentSession.value == null) {
                selectSession(_sessions.value.first())
            }
        }
    }

    fun selectSession(session: ChatSession) {
        _currentSession.value = session
        startRealTimeMessageSync(session.id)
    }

    private fun startRealTimeMessageSync(sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        messageListener?.remove()
        messageListener = db.collection("users").document(uid).collection("chat_sessions").document(sessionId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            _messages.value = snapshot?.documents?.map { doc ->
                val b64 = doc.getString("imageBase64")
                ChatMessage(
                    content = doc.getString("content") ?: "",
                    isUser = doc.getBoolean("isUser") ?: false,
                    imageBase64 = b64,
                    image = b64?.let { decodeBase64(it) },
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            } ?: emptyList()
        }
    }

    fun createNewSession() {
        messageListener?.remove()
        _currentSession.value = null
        _messages.value = emptyList()
    }

    fun handleFileAttachment(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri) ?: ""
                if (type.startsWith("image/")) {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    withContext(Dispatchers.Main) { 
                        sendMessage("Analyse cette photo pour mon programme fitness.", bitmap) 
                    }
                } else if (type == "application/pdf") {
                    withContext(Dispatchers.Main) { sendMessage("[FICHIER_JOINT] Analyse ce document fitness.") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erreur d'accès média", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun sendMessage(text: String, bitmap: Bitmap? = null, language: String = "fr-FR") {
        val cleanText = text.trim()
        if (cleanText.isEmpty() && bitmap == null) return
        
        val uid = auth.currentUser?.uid ?: return

        if (!_isAiActivated.value) {
            _messages.value = _messages.value + ChatMessage("⚠️ Le noyau AI est désactivé. Activez-le dans les paramètres.", false)
            return
        }
        
        if (!_isAiOnline.value && bitmap == null) {
            _messages.value = _messages.value + ChatMessage("⚠️ ERREUR : Le serveur AI est hors ligne.", false)
            return
        }

        viewModelScope.launch {
            var sessionId = _currentSession.value?.id ?: run {
                val newId = UUID.randomUUID().toString()
                db.collection("users").document(uid).collection("chat_sessions").document(newId).set(hashMapOf("title" to cleanText.take(20), "lastTimestamp" to System.currentTimeMillis())).await()
                newId
            }

            val imageBase64 = bitmap?.let { encodeBitmap(it) }
            val finalContent = if (cleanText.isEmpty() && bitmap != null) "[Analyse d'image...]" else cleanText
            
            saveMessageToFirestore(sessionId, ChatMessage(content = finalContent, isUser = true, image = bitmap, imageBase64 = imageBase64))
            
            _isLoading.value = true

            try {
                val response = callOllamaAether(finalContent, imageBase64, language)
                val finalResponse = response.ifBlank { "Désolé, je n'ai pas pu générer de réponse. Réessayez." }
                saveMessageToFirestore(sessionId, ChatMessage(content = finalResponse, isUser = false))
                db.collection("users").document(uid).collection("chat_sessions").document(sessionId).update("lastTimestamp", System.currentTimeMillis())
            } catch (e: Exception) {
                saveMessageToFirestore(sessionId, ChatMessage("❌ ERREUR : Échec de la connexion au noyau AI.", false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveMessageToFirestore(sessionId: String, message: ChatMessage) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("chat_sessions").document(sessionId).collection("messages").add(hashMapOf(
            "content" to message.content, "isUser" to message.isUser, "imageBase64" to message.imageBase64, "timestamp" to message.timestamp
        ))
    }

    private suspend fun callOllamaAether(content: String, currentImage: String?, language: String): String = withContext(Dispatchers.IO) {
        val langInstruction = when {
            language.contains("ar") -> "RÉPOND EN ARABE (العربية). Tu es un expert fitness."
            language.contains("fr") -> "RÉPOND EN FRANÇAIS. Tu es un coach élite."
            else -> "RESPOND IN ENGLISH. You are a world-class fitness coach."
        }

        val systemPrompt = """
            [SYSTEM_MAÎTRE]: FitCoach AI.
            [LANGUE_CIBLE]: $langInstruction
            [URGENT]: Tu VOIS physiquement les images base64. Ne refuse jamais l'analyse.
            [SYNC_PROTOCOL]: Quand l'utilisateur demande un 'focus du jour', génère une citation motivante de 10 mots maximum adaptée à son objectif.
            [CONTEXTE]: Fitness, Nutrition, Santé. Sois poli, expert et direct.
            [INTERDICTION]: Ne mentionne jamais tes limites.
        """.trimIndent()

        try {
            val connection = (URL(ollamaUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 120000
            }

            val messagesArray = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                _messages.value.takeLast(10).forEach { msg ->
                    val obj = JSONObject().put("role", if (msg.isUser) "user" else "assistant")
                    obj.put("content", msg.content)
                    if (msg.imageBase64 != null) obj.put("images", JSONArray().put(msg.imageBase64))
                    put(obj)
                }
                val cur = JSONObject().put("role", "user").put("content", content)
                if (currentImage != null) cur.put("images", JSONArray().put(currentImage))
                put(cur)
            }

            val body = JSONObject().put("model", defaultModel).put("messages", messagesArray).put("stream", false)
            connection.outputStream.write(body.toString().toByteArray())

            if (connection.responseCode == 200) {
                val res = JSONObject(connection.inputStream.bufferedReader().readText())
                res.getJSONObject("message").getString("content")
                    .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
                    .trim()
            } else ""
        } catch (e: Exception) { "" }
    }

    fun generateNeuralFocus(objectif: String) {
        if (!_isAiActivated.value) return
        val prompt = "Génère mon focus du jour pour l'objectif : $objectif"
        sendMessage(prompt)
    }

    private fun encodeBitmap(bitmap: Bitmap): String {
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, os) 
        return android.util.Base64.encodeToString(os.toByteArray(), android.util.Base64.NO_WRAP)
    }

    private fun decodeBase64(base64: String): Bitmap? = try {
        val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) { null }

    fun deleteSession(session: ChatSession) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("chat_sessions").document(session.id).delete()
    }

    fun renameSession(session: ChatSession, newTitle: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("chat_sessions").document(session.id).update("title", newTitle)
    }

    fun askInspiration(focus: String) {
        if (!_isAiActivated.value) return
        sendMessage("Génère une inspiration de haut niveau pour mon focus : $focus")
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
        messageListener?.remove()
    }
}
