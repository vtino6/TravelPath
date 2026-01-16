package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.channels.Channel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tino.travelpath.ui.components.OpenStreetMapView
import com.tino.travelpath.ui.components.MapMarker
import org.osmdroid.util.GeoPoint

@Composable
fun ActiveNavigationScreen(
    navController: NavController,
    routeId: String? = null
) {
    // Handle system back button
    BackHandler {
        navController.popBackStack()
    }
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val localViewModelStoreOwner = LocalViewModelStoreOwner.current
    val viewModelStoreOwner = activity ?: localViewModelStoreOwner
    
    if (viewModelStoreOwner == null) {
        return
    }
    
    val viewModel: RouteSelectionViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "route_selection",
        factory = RouteSelectionViewModelFactory(RoutesRepository())
    )
    
    val routes by viewModel.routes.collectAsState()
    val startingPoint by viewModel.startingPoint.collectAsState()
    val startingPointName by viewModel.startingPointName.collectAsState()
    var currentStepIndex by remember { mutableStateOf(0) }
    
    val currentRoute = remember(routes, routeId) {
        if (routeId != null) {
            routes.find { it.id == routeId }
        } else {
            routes.firstOrNull()
        }
    }
    
    if (currentRoute == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune route disponible")
        }
        return
    }
    
    val totalSteps = currentRoute.steps.size
    
    // Channel for swipe detection
    val swipeChannel = remember { Channel<Int>(Channel.CONFLATED) }
    
    // Handle swipe direction changes
    LaunchedEffect(Unit) {
        for (direction in swipeChannel) {
            when (direction) {
                1 -> { // Swipe left - next step
                    if (currentStepIndex < totalSteps - 1) {
                        currentStepIndex++
                    }
                }
                -1 -> { // Swipe right - previous step
                    if (currentStepIndex > 0) {
                        currentStepIndex--
                    }
                }
            }
        }
    }
    val currentStep = if (currentStepIndex < totalSteps) {
        currentRoute.steps[currentStepIndex]
    } else {
        null
    }
    
    if (currentStep == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Navigation terminée")
        }
        return
    }
    
    val progress = if (totalSteps > 0) {
        (currentStepIndex + 1).toFloat() / totalSteps.toFloat()
    } else {
        0f
    }
    
    // Calculate travel time and distance from starting point or previous step
    val distance = currentStep.distanceFromPrevious ?: 0.0
    val travelTimeMinutes = if (distance > 0) {
        ((distance / 5.0) * 60).toInt() // Walking speed: 5 km/h
    } else {
        0
    }
    
    val distanceText = if (currentStepIndex == 0) {
        // First step: from starting point
        if (distance > 0) {
            "${String.format("%.1f", distance)} km depuis le point de départ"
        } else {
            "Distance depuis le point de départ non disponible"
        }
    } else {
        // Subsequent steps: from previous step
        if (distance > 0) {
            "${String.format("%.1f", distance)} km depuis l'étape précédente"
        } else {
            "Distance depuis l'étape précédente non disponible"
        }
    }
    Scaffold(
        topBar = {
            // Step indicator in top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStepIndex) 10.dp else 6.dp)
                                .background(
                                    color = if (index == currentStepIndex) 
                                        Color(0xFF667eea) 
                                    else 
                                        Color(0xFFBDBDBD),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Floating toolbar with back button above map
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF667eea)
                            )
                        }
                    }
                }
                
                // Spacing between toolbar and map
                Spacer(modifier = Modifier.height(12.dp))
                
                // Map view with real route data
                val markers = mutableListOf<MapMarker>()
                
                // Add starting point if available
                startingPoint?.let { (lat, lng) ->
                    markers.add(
                        MapMarker(
                            latitude = lat,
                            longitude = lng,
                            title = "Point de départ",
                            isStartingPoint = true
                        )
                    )
                }
                
                // Add step markers
                currentRoute.steps.forEachIndexed { index, step ->
                    markers.add(
                        MapMarker(
                            latitude = step.place.latitude,
                            longitude = step.place.longitude,
                            title = if (index == currentStepIndex) "${step.place.name} (Actuel)" else step.place.name,
                            isStartingPoint = false
                        )
                    )
                }
                
                // Build polyline: starting point first (if available), then steps
                val routePolylinePoints = mutableListOf<GeoPoint>()
                startingPoint?.let { (lat, lng) ->
                    routePolylinePoints.add(GeoPoint(lat, lng))
                }
                currentRoute.steps.forEach { step ->
                    routePolylinePoints.add(
                        GeoPoint(step.place.latitude, step.place.longitude)
                    )
                }
                
                // Map container with clipping to prevent overflow and glitches
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OpenStreetMapView(
                        centerLatitude = currentStep.place.latitude,
                        centerLongitude = currentStep.place.longitude,
                        markers = markers,
                        routePolyline = routePolylinePoints.takeIf { it.isNotEmpty() },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                
                Spacer(modifier = Modifier.height(15.dp))
                
                // Progress indicator
                Text(
                    text = "Étape ${currentStepIndex + 1}/$totalSteps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    color = Color(0xFF667eea)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Steps list with swipe detection (only on list, not on map)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            var localDrag = 0f
                            detectHorizontalDragGestures { change, dragAmount ->
                                localDrag += dragAmount
                                // Trigger swipe when threshold is reached
                                if (localDrag > 150) {
                                    // Swipe right - previous step
                                    swipeChannel.trySend(-1)
                                    localDrag = 0f
                                } else if (localDrag < -150) {
                                    // Swipe left - next step
                                    swipeChannel.trySend(1)
                                    localDrag = 0f
                                }
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                // Starting point
                item {
                    StepCard(
                        title = "Point de départ: ${startingPointName ?: "Localisation sélectionnée"}",
                        address = null,
                        distanceText = null,
                        isCurrent = false,
                        isStartingPoint = true
                    )
                }
                
                // Route steps
                itemsIndexed(currentRoute.steps) { index, step ->
                    val distance = step.distanceFromPrevious ?: 0.0
                    val travelTimeMinutes = if (distance > 0) {
                        ((distance / 5.0) * 60).toInt()
                    } else {
                        0
                    }
                    
                    val distanceText = if (index == 0) {
                        if (distance > 0) {
                            "${String.format("%.1f", distance)} km depuis le point de départ"
                        } else {
                            null
                        }
                    } else {
                        if (distance > 0) {
                            "${String.format("%.1f", distance)} km depuis l'étape précédente"
                        } else {
                            null
                        }
                    }
                    
                    StepCard(
                        title = "${step.order}. ${step.place.name}",
                        address = step.place.address,
                        distanceText = distanceText,
                        isCurrent = index == currentStepIndex,
                        isStartingPoint = false
                    )
                }
            }
            
                Spacer(modifier = Modifier.weight(1f))
                
                // Navigation buttons at bottom (icons only)
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous step button (FAB style)
                FloatingActionButton(
                    onClick = { 
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (currentStepIndex > 0) 
                        Color(0xFF667eea) 
                    else 
                        Color(0xFFE0E0E0),
                    contentColor = if (currentStepIndex > 0) 
                        Color.White 
                    else 
                        Color(0xFF9E9E9E)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Étape précédente",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Next step button (FAB style)
                FloatingActionButton(
                    onClick = { 
                        if (currentStepIndex < totalSteps - 1) {
                            currentStepIndex++
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (currentStepIndex < totalSteps - 1) 
                        Color(0xFF667eea) 
                    else 
                        Color(0xFFE0E0E0),
                    contentColor = if (currentStepIndex < totalSteps - 1) 
                        Color.White 
                    else 
                        Color(0xFF9E9E9E)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Étape suivante",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    }
}

@Composable
fun StepCard(
    title: String,
    address: String?,
    distanceText: String?,
    isCurrent: Boolean,
    isStartingPoint: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFE3F2FD) else if (isStartingPoint) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF667eea)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/Number
            if (isStartingPoint) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            } else {
                // Extract step number from title (e.g., "1. Place Name" -> "1")
                val stepNumber = title.substringBefore(".").takeIf { it.matches(Regex("\\d+")) } ?: ""
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isCurrent) Color(0xFF667eea) else Color(0xFF9E9E9E),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (stepNumber.isNotEmpty()) {
                        Text(
                            text = stepNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) Color(0xFF667eea) else Color.Black
                )
                
                if (address != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                if (distanceText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = distanceText,
                        fontSize = 14.sp,
                        color = Color(0xFF667eea),
                        fontWeight = FontWeight.Medium
                    )
                }
                
            }
        }
    }
}

