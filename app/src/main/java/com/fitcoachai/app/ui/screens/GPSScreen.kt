package com.fitcoachai.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitcoachai.app.ui.components.PremiumBackground
import com.fitcoachai.app.ui.components.PremiumGlassBar
import com.fitcoachai.app.ui.components.PremiumLiveBadge
import com.fitcoachai.app.viewmodel.GpsViewModel
import com.fitcoachai.app.viewmodel.MapLatLng
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.utils.ColorUtils
import java.util.Calendar

@Composable
fun GPSTrackerScreen(
    gpsViewModel: GpsViewModel = viewModel(),
    onShowHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTracking by gpsViewModel.isTracking.collectAsState()
    val currentLocation by gpsViewModel.currentLocation.collectAsState()
    val routePoints by gpsViewModel.routePoints.collectAsState()
    val distanceKm by gpsViewModel.distanceKm.collectAsState()
    val elapsedSeconds by gpsViewModel.elapsedSeconds.collectAsState()
    val currentSpeed by gpsViewModel.currentSpeedKmh.collectAsState()
    val currentBearing by gpsViewModel.currentBearing.collectAsState()
    val calories by gpsViewModel.calories.collectAsState()
    val isAutoCenterEnabled by gpsViewModel.isAutoCenterEnabled.collectAsState()
    val paceMinPerKm by gpsViewModel.paceMinPerKm.collectAsState()
    val mapStyleKey by gpsViewModel.mapStyle.collectAsState()
    val speedHistory by gpsViewModel.speedHistory.collectAsState()
    val splits by gpsViewModel.splits.collectAsState()
    val altitude by gpsViewModel.altitude.collectAsState()
    val isSafetyModeEnabled by gpsViewModel.isSafetyModeEnabled.collectAsState()
    val weatherInfo by gpsViewModel.weatherInfo.collectAsState()

    var showAnalytics by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            currentTime = "%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            delay(1000)
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var lineManager by remember { mutableStateOf<LineManager?>(null) }
    val mapView = remember { MapView(context) }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulseAlpha"
    )

    // Lifecycle Sync
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    if (hasLocationPermission) gpsViewModel.startLocationUpdates()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    gpsViewModel.stopLocationUpdates()
                }
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                              permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission, mapLibreMap) {
        if (hasLocationPermission) {
            gpsViewModel.startLocationUpdates()
            mapLibreMap?.getStyle { style ->
                enableLocationComponent(mapLibreMap!!, style, context)
            }
        }
    }

    LaunchedEffect(mapStyleKey) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val url = if (mapStyleKey == "dark") "https://tiles.openfreemap.org/styles/dark" 
                  else "https://tiles.openfreemap.org/styles/bright" 
        
        map.setStyle(url) { style ->
            lineManager = LineManager(mapView, map, style)
            if (hasLocationPermission) enableLocationComponent(map, style, context)
            if (routePoints.size > 1) {
                val points = routePoints.map { LatLng(it.latitude, it.longitude) }
                lineManager?.create(LineOptions().withLatLngs(points).withLineColor("#00D4FF").withLineWidth(6f))
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle("https://tiles.openfreemap.org/styles/dark") { style ->
                lineManager = LineManager(mapView, map, style)
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = false
                if (hasLocationPermission) enableLocationComponent(map, style, context)
                map.addOnCameraMoveStartedListener { reason ->
                    if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        gpsViewModel.setAutoCenter(false)
                    }
                }
            }
        }
    }

    LaunchedEffect(routePoints) {
        val manager = lineManager ?: return@LaunchedEffect
        // Only redraw if we have new points and manager is ready
        if (routePoints.size > 1) {
            val points = routePoints.map { LatLng(it.latitude, it.longitude) }
            manager.deleteAll() // MapLibre annotations don't support partial updates easily, but we can minimize frequency
            manager.create(
                LineOptions()
                    .withLatLngs(points)
                    .withLineColor(ColorUtils.colorToRgbaString(android.graphics.Color.parseColor("#00D4FF")))
                    .withLineWidth(6f)
            )
        }
    }

    LaunchedEffect(currentLocation, isAutoCenterEnabled) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (isAutoCenterEnabled) {
            currentLocation?.let { loc ->
                val targetZoom = if (currentSpeed > 15) 16.0 else 18.0
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(loc.latitude, loc.longitude))
                    .zoom(targetZoom)
                    .bearing(currentBearing.toDouble())
                    .tilt(60.0) 
                    .build()
                // Reduced duration to match update frequency (500ms)
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 500)
            }
        }
    }

    val timeFormatted = remember(elapsedSeconds) {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    val paceFormatted = remember(paceMinPerKm) {
        if (paceMinPerKm > 0 && paceMinPerKm < 60) {
            val min = paceMinPerKm.toInt()
            val sec = ((paceMinPerKm - min) * 60).toInt()
            "%d'%02d\"".format(min, sec)
        } else "--'--\""
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 🗺 FULL MAP LAYER
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Immersive gradient overlays (Subtle)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent, Color.Black.copy(0.4f)))))

        // 🛡 TOP HUD BANNER (RE-DESIGNED)
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            PremiumGlassBar(
                modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT: Live Clock
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("HEURE", color = Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(currentTime, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }

                    // CENTER: Status Badge
                    Surface(
                        color = (if (isTracking) Color(0xFF00C853) else Color(0xFFFFD700)).copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, (if (isTracking) Color(0xFF00C853) else Color(0xFFFFD700)).copy(0.3f))
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(if (isTracking) Color(0xFF00C853) else Color(0xFFFFD700), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isTracking) "ACTIVE" else "READY",
                                color = if (isTracking) Color(0xFF00C853) else Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // RIGHT: Weather Chip
                    Column(horizontalAlignment = Alignment.End) {
                        val weatherParts = weatherInfo.split(" • ")
                        val temp = weatherParts.getOrNull(0) ?: "--°C"
                        val condition = weatherParts.getOrNull(1) ?: "..."
                        
                        Text(condition.uppercase(), color = Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(temp, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            
            // QUICK ACTION BAR (Floating under main banner)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.background(Color.Black.copy(0.5f), RoundedCornerShape(16.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HUDIconButton(icon = if (isSafetyModeEnabled) Icons.Default.FlashOn else Icons.Default.FlashlightOn, active = isSafetyModeEnabled) {
                        gpsViewModel.toggleSafetyMode()
                    }
                    HUDIconButton(icon = Icons.Default.Map) {
                        gpsViewModel.toggleMapStyle()
                    }
                    HUDIconButton(icon = Icons.Default.History) {
                        onShowHistory()
                    }
                }
            }
        }

        // 📊 BOTTOM FLOATING HUD (MINIMALIST)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Analytics Drawer (Slide up)
            AnimatedVisibility(
                visible = showAnalytics,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("ANALYTIQUES EN DIRECT", color = Color(0xFF00D4FF), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.height(100.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SpeedChart(Modifier.weight(2f), speedHistory)
                            Column(Modifier.weight(1f)) {
                                Text("ÉLÉVATION", color = Color.White.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("${altitude.toInt()}m", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(8.dp))
                                Text("SPLITS", color = Color.White.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Box(Modifier.weight(1f)) { SplitsList(splits) }
                            }
                        }
                    }
                }
            }

            // Main Stats HUD (Compact Glass)
            Surface(
                color = Color.White.copy(0.08f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.15f)),
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(32.dp))
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatHUDItem("KM", "%.2f".format(distanceKm), Color(0xFF00D4FF))
                    StatHUDItem("TIME", timeFormatted, Color.White)
                    StatHUDItem("KM/H", "%.1f".format(currentSpeed), Color(0xFFFF6B35))
                    
                    // Toggle Analytics
                    IconButton(
                        onClick = { showAnalytics = !showAnalytics },
                        modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(if (showAnalytics) Icons.Default.Close else Icons.Default.Analytics, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // BIG FLOATING ACTION BUTTON
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // Secondary Map Controls (Left)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HUDCircleButton(icon = if (isAutoCenterEnabled) Icons.Default.MyLocation else Icons.Default.LocationSearching, active = isAutoCenterEnabled) {
                        gpsViewModel.setAutoCenter(!isAutoCenterEnabled)
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // MAIN START/STOP BUTTON
                PremiumGpsActionButton(
                    isTracking = isTracking,
                    modifier = Modifier.width(220.dp).height(70.dp),
                    onClick = { if (isTracking) gpsViewModel.stopTracking() else gpsViewModel.startTracking() }
                )

                Spacer(Modifier.weight(1f))

                // Compass/Center (Right)
                Box(contentAlignment = Alignment.Center) {
                    if (isTracking) {
                        Box(Modifier.size(56.dp * pulseScale).graphicsLayer(alpha = pulseAlpha).background(Color(0xFF00D4FF).copy(0.3f), CircleShape))
                    }
                    HUDCircleButton(icon = Icons.Default.Navigation, color = Color.White, iconColor = Color.Black) {
                        gpsViewModel.setAutoCenter(true)
                        currentLocation?.let { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) }
                    }
                }
            }
        }

        // Permission Overlay
        if (!hasLocationPermission) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.9f)).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF00D4FF), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(24.dp))
                    Text("GPS ÉLITE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Accès position requis pour le suivi", color = Color.White.copy(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))) {
                        Text("AUTORISER", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HUDIconButton(icon: ImageVector, active: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).background(if (active) Color(0xFF00D4FF).copy(0.2f) else Color.White.copy(0.08f), CircleShape)
    ) {
        Icon(icon, null, tint = if (active) Color(0xFF00D4FF) else Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun HUDCircleButton(icon: ImageVector, active: Boolean = false, color: Color = Color.White.copy(0.1f), iconColor: Color = Color.White, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        color = if (active) Color(0xFF00D4FF) else color,
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (active) Color.Black else iconColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun StatHUDItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SpeedChart(modifier: Modifier, history: List<Double>) {
    Spacer(modifier = modifier
        .fillMaxHeight()
        .background(Color.White.copy(0.02f), RoundedCornerShape(12.dp))
        .drawWithCache {
            val strokePath = Path()
            val fillPath = Path()
            
            onDrawBehind {
                if (history.size < 2) return@onDrawBehind
                
                val maxSpeed = (history.maxOrNull() ?: 10.0).coerceAtLeast(10.0)
                val width = size.width
                val height = size.height
                val stepX = width / (history.size - 1)
                
                strokePath.reset()
                fillPath.reset()
                
                history.forEachIndexed { i, speed ->
                    val x = i * stepX
                    val y = height - (speed / maxSpeed * height).toFloat()
                    if (i == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(width, height)
                fillPath.close()
                
                drawPath(fillPath, Brush.verticalGradient(listOf(Color(0xFF00D4FF).copy(0.3f), Color.Transparent)))
                drawPath(strokePath, Color(0xFF00D4FF), style = Stroke(4f))
            }
        }
    )
}

@Composable
fun SplitsList(splits: List<Double>) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(splits.size) { index ->
            val d = splits[index].toLong(); val m = d/60; val s = d%60
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}K", color = Color.White.copy(0.3f), fontSize = 9.sp)
                Text("%d'%02d\"".format(m, s), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableLocationComponent(map: MapLibreMap, style: Style, context: android.content.Context) {
    val options = LocationComponentOptions.builder(context)
        .accuracyAlpha(0.15f)
        .accuracyColor(android.graphics.Color.parseColor("#00D4FF"))
        .foregroundTintColor(android.graphics.Color.parseColor("#00D4FF"))
        .build()
    map.locationComponent.let { component ->
        component.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).locationComponentOptions(options).useDefaultLocationEngine(true).build())
        component.isLocationComponentEnabled = true
        component.cameraMode = CameraMode.NONE
        component.renderMode = RenderMode.COMPASS
    }
}

@Composable
fun PremiumGpsActionButton(isTracking: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "btnScale")
    val color = if (isTracking) Color(0xFFFF3B30) else Color(0xFF00D4FF)
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale).shadow(if (isTracking) 20.dp else 0.dp, CircleShape, spotColor = color),
        color = color,
        shape = CircleShape
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = if (isTracking) Color.White else Color.Black)
            Spacer(Modifier.width(12.dp))
            Text(if (isTracking) "ARRÊTER" else "LANCER LA COURSE", color = if (isTracking) Color.White else Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
        }
    }
}
