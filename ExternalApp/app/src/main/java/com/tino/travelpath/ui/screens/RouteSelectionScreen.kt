package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tino.travelpath.ui.components.OpenStreetMapView
import com.tino.travelpath.ui.components.MapMarker
import org.osmdroid.util.GeoPoint
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.tino.travelpath.R
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.data.database.TravelPathDatabase
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun RouteSelectionScreen(
    navController: NavController
) {
    // Handle system back button
    BackHandler {
        navController.popBackStack()
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val localViewModelStoreOwner = LocalViewModelStoreOwner.current
    
    android.util.Log.d("RouteSelectionScreen", "Activity: $activity")
    android.util.Log.d("RouteSelectionScreen", "Local ViewModelStoreOwner: $localViewModelStoreOwner")
    
    val viewModelStoreOwner = activity ?: localViewModelStoreOwner
    
    if (viewModelStoreOwner == null) {
        android.util.Log.e("RouteSelectionScreen", "ERROR: No ViewModelStoreOwner available!")
        return
    }
    val database = remember { TravelPathDatabase.getDatabase(context) }
    val viewModel: RouteSelectionViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "route_selection",
        factory = RouteSelectionViewModelFactory(
            RoutesRepository(parcoursDao = database.parcoursDao())
        )
    )
    
    android.util.Log.d("RouteSelectionScreen", "Got RouteSelectionViewModel: $viewModel")
    android.util.Log.d("RouteSelectionScreen", "ViewModel hash: ${viewModel.hashCode()}")
    val routes by viewModel.routes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val currentUserIdState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val database = TravelPathDatabase.getDatabase(context)
            val users = database.utilisateurDao().getAllFlow().firstOrNull()
            currentUserIdState.value = users?.firstOrNull()?.id
            android.util.Log.d("RouteSelectionScreen", "Current user ID: ${currentUserIdState.value}")
        } catch (e: Exception) {
            android.util.Log.e("RouteSelectionScreen", "Error getting current user", e)
        }
    }
    val currentUserId = currentUserIdState.value
    
    android.util.Log.d("RouteSelectionScreen", "=== ROUTE SELECTION SCREEN STATE ===")
    android.util.Log.d("RouteSelectionScreen", "Routes count: ${routes.size}")
    android.util.Log.d("RouteSelectionScreen", "Is loading: $isLoading")
    android.util.Log.d("RouteSelectionScreen", "Error: $error")
    routes.forEachIndexed { index, route ->
        android.util.Log.d("RouteSelectionScreen", "Route $index: ${route.name} (${route.routeType})")
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(stringResource(id = R.string.route_selection_back_button))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = "Erreur: $error",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (routes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aucun itinéraire trouvé",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Aucun itinéraire ne respecte vos contraintes.\nEssayez d'augmenter le budget ou la durée disponible.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea)
                        )
                    ) {
                        Text("Modifier les contraintes", color = Color.White)
                    }
                }
            } else {
                Text(
                    text = "${routes.size} itinéraire(s) trouvé(s)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                val selectedModes by viewModel.selectedTransportationModes.collectAsState()
                val startingPoint by viewModel.startingPoint.collectAsState()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    items(routes) { route ->
                        RouteCardItem(
                            route = route,
                            startingPoint = startingPoint,
                            onViewDetails = { 
                                navController.navigate("route_detail/${route.id}") 
                            },
                            onLike = {
                                viewModel.toggleLike(route, currentUserId)
                                android.util.Log.d("RouteSelectionScreen", "Toggled like for route ${route.id}, isFavorite: ${route.isFavorite}")
                            },
                            selectedTransportationModes = selectedModes
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCardItem(
    route: RouteResponse,
    startingPoint: Pair<Double, Double>? = null,
    onViewDetails: () -> Unit,
    onLike: () -> Unit,
    selectedTransportationModes: Set<com.tino.travelpath.data.database.entities.TransportationMode> = emptySet()
) {
    val routeTypeName = when (route.routeType) {
        "ECONOMIC", "ECONOMICAL" -> "Économique"
        "BALANCED" -> "Équilibré"
        "COMFORT" -> "Confort"
        else -> route.name
    }
    
    val budgetFormatted = "%.0f€".format(route.totalBudget)
    val stepsCount = route.steps.size
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Mini map with route data and like button overlaid on top-right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Map as background
                if (route.steps.isNotEmpty() || startingPoint != null) {
                    // Build markers: starting point first (if available), then steps
                    val markers = mutableListOf<MapMarker>()
                    
                    // Add starting point as first marker (distinct)
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
                    route.steps.forEach { step ->
                        markers.add(
                            MapMarker(
                                latitude = step.place.latitude,
                                longitude = step.place.longitude,
                                title = step.place.name,
                                isStartingPoint = false
                            )
                        )
                    }
                    
                    // Build polyline: starting point first (if available), then steps
                    val routePolylinePoints = mutableListOf<GeoPoint>()
                    startingPoint?.let { (lat, lng) ->
                        routePolylinePoints.add(GeoPoint(lat, lng))
                    }
                    route.steps.forEach { step ->
                        routePolylinePoints.add(
                            GeoPoint(step.place.latitude, step.place.longitude)
                        )
                    }
                    
                    // Center map on starting point if available, otherwise first step
                    val centerLat = startingPoint?.first ?: route.steps.firstOrNull()?.place?.latitude ?: 0.0
                    val centerLng = startingPoint?.second ?: route.steps.firstOrNull()?.place?.longitude ?: 0.0
                    
                    OpenStreetMapView(
                        centerLatitude = centerLat,
                        centerLongitude = centerLng,
                        markers = markers,
                        routePolyline = routePolylinePoints.takeIf { it.isNotEmpty() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    // Fallback if no steps or starting point
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune étape disponible", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                
                // Like button overlaid on top-right corner
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .padding(8.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (route.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (route.isFavorite == true) "Retirer des favoris" else "Ajouter aux favoris",
                        modifier = Modifier.size(24.dp),
                        tint = if (route.isFavorite == true) Color(0xFFE91E63) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = routeTypeName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    // Budget card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF667eea)
                        )
                        Column {
                            Text("$budgetFormatted", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("(estimation)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    
                    // Transportation modes - show icons based on selected modes and step distances
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        getTransportationModeIcons(
                            mode = route.transportationMode ?: "MIXED",
                            selectedModes = selectedTransportationModes,
                            steps = route.steps
                        ).forEach { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF667eea)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$stepsCount lieux (calculé automatiquement)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF667eea),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // View details button only (like button is now on the image)
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text(
                    stringResource(id = R.string.route_selection_view_details_button),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Get transportation mode icons based on:
 * 1. Selected modes from user preferences (only show what was selected)
 * 2. Step distances (only show modes that make sense for the distances)
 */
private fun getTransportationModeIcons(
    mode: String,
    selectedModes: Set<com.tino.travelpath.data.database.entities.TransportationMode>,
    steps: List<com.tino.travelpath.data.api.dto.StepResponse>
): List<androidx.compose.ui.graphics.vector.ImageVector> {
    // If it's a single mode (not MIXED), show only that mode if it was selected
    val singleMode = when (mode.uppercase()) {
        "WALKING" -> {
            if (selectedModes.contains(com.tino.travelpath.data.database.entities.TransportationMode.WALKING) || selectedModes.isEmpty()) {
                listOf(Icons.Default.DirectionsWalk)
            } else {
                emptyList()
            }
        }
        "BICYCLE" -> {
            if (selectedModes.contains(com.tino.travelpath.data.database.entities.TransportationMode.BICYCLE) || selectedModes.isEmpty()) {
                listOf(Icons.Default.DirectionsBike)
            } else {
                emptyList()
            }
        }
        "PUBLIC_TRANSPORT" -> {
            if (selectedModes.contains(com.tino.travelpath.data.database.entities.TransportationMode.PUBLIC_TRANSPORT) || selectedModes.isEmpty()) {
                listOf(Icons.Default.Train)
            } else {
                emptyList()
            }
        }
        "CAR" -> {
            if (selectedModes.contains(com.tino.travelpath.data.database.entities.TransportationMode.CAR) || selectedModes.isEmpty()) {
                listOf(Icons.Default.DirectionsCar)
            } else {
                emptyList()
            }
        }
        else -> emptyList()
    }
    
    if (singleMode.isNotEmpty()) {
        return singleMode
    }
    
    // For MIXED mode, filter by selected modes and step distances
    if (mode.uppercase() == "MIXED") {
        val relevantModes = mutableListOf<androidx.compose.ui.graphics.vector.ImageVector>()
        
        // Get all step distances
        val stepDistances = steps.mapNotNull { it.distanceFromPrevious }
        
        // If no selected modes, infer from distances (fallback)
        val modesToCheck = if (selectedModes.isEmpty()) {
            setOf(
                com.tino.travelpath.data.database.entities.TransportationMode.WALKING,
                com.tino.travelpath.data.database.entities.TransportationMode.BICYCLE,
                com.tino.travelpath.data.database.entities.TransportationMode.PUBLIC_TRANSPORT,
                com.tino.travelpath.data.database.entities.TransportationMode.CAR
            )
        } else {
            selectedModes
        }
        
        // Check each selected mode against step distances
        modesToCheck.forEach { selectedMode ->
            val isRelevant = when (selectedMode) {
                com.tino.travelpath.data.database.entities.TransportationMode.WALKING -> {
                    // Walking is relevant for short distances (< 2km)
                    stepDistances.isEmpty() || stepDistances.any { it < 2.0 } || stepDistances.all { it < 2.0 }
                }
                com.tino.travelpath.data.database.entities.TransportationMode.BICYCLE -> {
                    // Bicycle is relevant for medium distances (1-5km)
                    stepDistances.isEmpty() || stepDistances.any { it in 1.0..5.0 }
                }
                com.tino.travelpath.data.database.entities.TransportationMode.PUBLIC_TRANSPORT -> {
                    // Public transport is relevant for medium-long distances (2-10km)
                    stepDistances.isEmpty() || stepDistances.any { it in 2.0..10.0 }
                }
                com.tino.travelpath.data.database.entities.TransportationMode.CAR -> {
                    // Car is relevant for long distances (> 5km)
                    stepDistances.isEmpty() || stepDistances.any { it > 5.0 }
                }
                else -> false
            }
            
            if (isRelevant) {
                when (selectedMode) {
                    com.tino.travelpath.data.database.entities.TransportationMode.WALKING -> relevantModes.add(Icons.Default.DirectionsWalk)
                    com.tino.travelpath.data.database.entities.TransportationMode.BICYCLE -> relevantModes.add(Icons.Default.DirectionsBike)
                    com.tino.travelpath.data.database.entities.TransportationMode.PUBLIC_TRANSPORT -> relevantModes.add(Icons.Default.Train)
                    com.tino.travelpath.data.database.entities.TransportationMode.CAR -> relevantModes.add(Icons.Default.DirectionsCar)
                    else -> { /* Skip MIXED enum value */ }
                }
            }
        }
        
        // If no modes found but we have steps, show walking as default
        return if (relevantModes.isEmpty() && steps.isNotEmpty()) {
            listOf(Icons.Default.DirectionsWalk)
        } else {
            relevantModes
        }
    }
    
    // Default fallback
    return listOf(Icons.Default.Train)
}
