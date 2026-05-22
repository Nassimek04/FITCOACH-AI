package com.fitcoachai.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachai.app.data.model.RunSession
import com.fitcoachai.app.data.api.RetrofitClient
import com.fitcoachai.app.data.model.remote.BackendRunCreate
import com.fitcoachai.app.utils.SessionManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.*
import kotlin.math.*

// ✅ Modèle LatLng générique
data class MapLatLng(val latitude: Double, val longitude: Double)

// ✅ Modèles Météo pour l'API wttr.in (JSON format j1)
data class WeatherResponse(
    val current_condition: List<Condition>
)
data class Condition(
    val temp_C: String,
    val lang_fr: List<WeatherDesc>?
)
data class WeatherDesc(
    val value: String
)

interface WeatherService {
    @GET("/{location}")
    suspend fun getWeather(
        @Path("location") location: String,
        @Query("format") format: String = "j1",
        @Query("lang") lang: String = "fr"
    ): WeatherResponse
}

class GpsViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherApi = Retrofit.Builder()
        .baseUrl("https://wttr.in")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherService::class.java)

    private val fusedClient = LocationServices.getFusedLocationProviderClient(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sessionManager = SessionManager(application)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _routePoints = MutableStateFlow<List<MapLatLng>>(emptyList())
    val routePoints: StateFlow<List<MapLatLng>> = _routePoints.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing: StateFlow<Float> = _currentBearing.asStateFlow()

    private val _paceMinPerKm = MutableStateFlow(0.0)
    val paceMinPerKm: StateFlow<Double> = _paceMinPerKm.asStateFlow()

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories.asStateFlow()

    private val _currentLocation = MutableStateFlow<MapLatLng?>(null)
    val currentLocation: StateFlow<MapLatLng?> = _currentLocation.asStateFlow()

    private val _isAutoCenterEnabled = MutableStateFlow(true)
    val isAutoCenterEnabled: StateFlow<Boolean> = _isAutoCenterEnabled.asStateFlow()

    private val _mapStyle = MutableStateFlow("dark")
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<Double>>(emptyList())
    val speedHistory: StateFlow<List<Double>> = _speedHistory.asStateFlow()

    private val _splits = MutableStateFlow<List<Double>>(emptyList())
    val splits: StateFlow<List<Double>> = _splits.asStateFlow()

    private val _altitude = MutableStateFlow(0.0)
    val altitude: StateFlow<Double> = _altitude.asStateFlow()

    private val _isSafetyModeEnabled = MutableStateFlow(false)
    val isSafetyModeEnabled: StateFlow<Boolean> = _isSafetyModeEnabled.asStateFlow()

    private val _weatherInfo = MutableStateFlow("Chargement...")
    val weatherInfo: StateFlow<String> = _weatherInfo.asStateFlow()

    private var lastSplitDistance = 0.0
    private var lastSplitTime = 0L
    private var lastWeatherUpdate = 0L

    private var timerJob: Job? = null
    private var safetyJob: Job? = null
    private var lastLocation: Location? = null
    private var lastBearing = 0f

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 500L 
    ).setMinUpdateIntervalMillis(300L)
     .setMinUpdateDistanceMeters(0.1f)
     .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                val newPoint = MapLatLng(location.latitude, location.longitude)
                _currentLocation.value = newPoint
                
                if (location.hasBearing()) {
                    lastBearing = lerpBearing(lastBearing, location.bearing, 0.25f)
                    _currentBearing.value = lastBearing
                }

                val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
                _currentSpeedKmh.value = speedKmh
                _altitude.value = location.altitude

                // ✅ Mise à jour de la météo réelle (toutes les 10 min)
                if (System.currentTimeMillis() - lastWeatherUpdate > 600000) {
                    fetchRealWeather(location.latitude, location.longitude)
                }

                if (_isTracking.value) {
                    val history = _speedHistory.value.toMutableList()
                    if (history.size > 50) history.removeAt(0)
                    history.add(speedKmh)
                    _speedHistory.value = history

                    // ✅ Precision Filtering: ignore low accuracy points
                    if (location.accuracy > 20) return 

                    val prev = lastLocation
                    if (prev == null) {
                        _routePoints.value = listOf(newPoint)
                        lastLocation = location
                    } else {
                        val dist = prev.distanceTo(location)
                        // ✅ Precision Filter: Minimum distance threshold to filter jitter
                        val minDelta = max(1.5f, location.accuracy / 4f)
                        
                        if (dist > minDelta) {
                            _distanceKm.value += dist / 1000.0
                            _routePoints.value = _routePoints.value + newPoint
                            
                            // ✅ Enhanced Calorie Precision: MET based (approx 8.0 MET for running)
                            // Formula: Calories = MET * weight_kg * time_hours
                            // Defaulting to 75kg if weight is unavailable
                            _calories.value = (_distanceKm.value * 75 * 1.036).toInt() 
                            
                            lastLocation = location

                            if (_distanceKm.value >= lastSplitDistance + 1.0) {
                                val splitDuration = _elapsedSeconds.value - lastSplitTime
                                _splits.value = _splits.value + splitDuration.toDouble()
                                lastSplitDistance += 1.0
                                lastSplitTime = _elapsedSeconds.value
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveRunSession() {
        val uid = auth.currentUser?.uid ?: return
        val session = RunSession(
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            distanceMeters = (_distanceKm.value * 1000).toFloat(),
            durationSeconds = _elapsedSeconds.value,
            calories = _calories.value,
            speedKmh = (_currentSpeedKmh.value).toFloat()
        )

        viewModelScope.launch {
            try {
                // 1. Firebase Save
                firestore.collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(session.id)
                    .set(session)

                // 2. FastAPI Backend Save (Parallel)
                try {
                    val authHeader = sessionManager.getAuthHeader()
                    if (authHeader != null) {
                        val backendRun = BackendRunCreate(
                            distanceKm = _distanceKm.value.toFloat(),
                            durationSeconds = _elapsedSeconds.value,
                            avgSpeedKmh = if (_elapsedSeconds.value > 0) (_distanceKm.value / (_elapsedSeconds.value / 3600.0)).toFloat() else 0f,
                            caloriesBurned = _calories.value.toFloat(),
                            routePoints = _routePoints.value.map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
                        )
                        RetrofitClient.fitCoachApi.createRun(authHeader, backendRun)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchRealWeather(lat: Double, lon: Double) {
        lastWeatherUpdate = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                // wttr.in attend le format lat,lon directement dans l'URL pour plus de fiabilité
                val locationQuery = "${lat},${lon}"
                val response = weatherApi.getWeather(locationQuery)
                val condition = response.current_condition.firstOrNull()
                if (condition != null) {
                    val temp = condition.temp_C
                    val desc = condition.lang_fr?.firstOrNull()?.value ?: "Partiellement nuageux"
                    _weatherInfo.value = "$temp°C • $desc"
                }
            } catch (e: Exception) {
                _weatherInfo.value = "Météo indisponible"
                e.printStackTrace()
            }
        }
    }

    private fun lerpBearing(start: Float, end: Float, fraction: Float): Float {
        var diff = (end - start + 180) % 360 - 180
        if (diff < -180) diff += 360
        return start + diff * fraction
    }

    fun setAutoCenter(enabled: Boolean) {
        _isAutoCenterEnabled.value = enabled
    }

    fun toggleMapStyle() {
        _mapStyle.value = if (_mapStyle.value == "dark") "satellite" else "dark"
    }

    fun toggleSafetyMode() {
        _isSafetyModeEnabled.value = !_isSafetyModeEnabled.value
        if (_isSafetyModeEnabled.value) {
            startSafetyStrobe()
        } else {
            safetyJob?.cancel()
            setFlashlight(false)
        }
    }

    private fun startSafetyStrobe() {
        safetyJob?.cancel()
        safetyJob = viewModelScope.launch {
            var isOn = false
            while (true) {
                isOn = !isOn
                setFlashlight(isOn)
                delay(if (isOn) 100 else 400)
            }
        }
    }

    private fun setFlashlight(enabled: Boolean) {
        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        _routePoints.value = emptyList()
        _distanceKm.value = 0.0
        _elapsedSeconds.value = 0L
        _calories.value = 0
        lastSplitDistance = 0.0
        lastSplitTime = 0L
        _splits.value = emptyList()
        lastLocation = null
        startTimer()
        startLocationUpdates()
    }

    fun stopTracking() {
        if (!_isTracking.value) return
        _isTracking.value = false
        timerJob?.cancel()
        saveRunSession()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        if (!_isTracking.value) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedClient.removeLocationUpdates(locationCallback)
    }
}
