package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.tino.travelpath.data.database.entities.TransportationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.ui.viewmodels.RouteFilter
import com.tino.travelpath.ui.viewmodels.SavedRoutesViewModel
import com.tino.travelpath.ui.viewmodels.SavedRoutesViewModelFactory
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.tino.travelpath.data.database.TravelPathDatabase
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun SavedRoutesScreen(
    navController: NavController,
    viewModel: SavedRoutesViewModel = viewModel(
        factory = SavedRoutesViewModelFactory(
            RoutesRepository(parcoursDao = TravelPathDatabase.getDatabase(LocalContext.current).parcoursDao())
        )
    ),
    routeSelectionViewModel: RouteSelectionViewModel? = null
) {
    // Handle system back button
    BackHandler {
        navController.popBackStack()
    }
    
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val localViewModelStoreOwner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current
    val viewModelStoreOwner = activity ?: localViewModelStoreOwner
    
    // Get current user ID (same as RouteSelectionScreen)
    val currentUserIdState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val database = com.tino.travelpath.data.database.TravelPathDatabase.getDatabase(context)
            val users = database.utilisateurDao().getAllFlow().firstOrNull()
            currentUserIdState.value = users?.firstOrNull()?.id
            android.util.Log.d("SavedRoutesScreen", "Current user ID: ${currentUserIdState.value}")
        } catch (e: Exception) {
            android.util.Log.e("SavedRoutesScreen", "Error getting current user", e)
        }
    }
    val currentUserId = currentUserIdState.value
    
    // Get RouteSelectionViewModel to access cached routes
    val database = remember { TravelPathDatabase.getDatabase(context) }
    val routeSelectionVM: RouteSelectionViewModel? = routeSelectionViewModel ?: if (viewModelStoreOwner != null) {
        val vm = androidx.lifecycle.viewmodel.compose.viewModel<RouteSelectionViewModel>(
            viewModelStoreOwner = viewModelStoreOwner,
            key = "route_selection",
            factory = RouteSelectionViewModelFactory(
                RoutesRepository(parcoursDao = database.parcoursDao())
            )
        )
        android.util.Log.d("SavedRoutesScreen", "Successfully got RouteSelectionViewModel: ${vm.hashCode()}")
        vm
    } else {
        android.util.Log.w("SavedRoutesScreen", "No ViewModelStoreOwner available")
        null
    }
    
    // Get cached routes from RouteSelectionViewModel (in-memory, not saved to DB) for "Tous" tab
    val routeSelectionRoutes by if (routeSelectionVM != null) {
        routeSelectionVM.routes.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<RouteResponse>()) }
    }
    
    // Update cached routes in SavedRoutesViewModel when RouteSelectionViewModel routes change
    androidx.compose.runtime.LaunchedEffect(routeSelectionRoutes) {
        if (routeSelectionVM != null) {
            android.util.Log.d("SavedRoutesScreen", "Got ${routeSelectionRoutes.size} cached routes from RouteSelectionViewModel")
            android.util.Log.d("SavedRoutesScreen", "Route IDs: ${routeSelectionRoutes.map { it.id }}")
            viewModel.setCachedRoutes(routeSelectionRoutes)
        }
    }
    
    val cachedRoutes by viewModel.cachedRoutes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    // Load liked routes (Likes tab) when screen appears - Tous tab uses cached routes from ViewModel
    androidx.compose.runtime.LaunchedEffect(currentUserId) {
        android.util.Log.d("SavedRoutesScreen", "Screen appeared, loading liked routes with userId: $currentUserId")
        viewModel.loadLikedRoutes(currentUserId) // Load liked routes for Likes tab (user-specific)
    }
    
    // Refresh liked routes when switching to Likes tab
    androidx.compose.runtime.LaunchedEffect(selectedFilter, currentUserId) {
        if (selectedFilter == RouteFilter.LIKES) {
            android.util.Log.d("SavedRoutesScreen", "Switched to LIKES tab, refreshing liked routes with userId: $currentUserId")
            viewModel.loadLikedRoutes(currentUserId)
        }
    }
    
    val filteredRoutes = viewModel.getFilteredRoutes()
    
    // Debug logging
    androidx.compose.runtime.LaunchedEffect(selectedFilter, filteredRoutes) {
        android.util.Log.d("SavedRoutesScreen", "Filter: $selectedFilter, Filtered routes: ${filteredRoutes.size}")
        android.util.Log.d("SavedRoutesScreen", "Cached routes: ${viewModel.cachedRoutes.value.size}, Liked routes: ${viewModel.likedRoutes.value.size}")
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mes Parcours",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = "saved_routes")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == RouteFilter.TOUS,
                    onClick = { 
                        viewModel.setFilter(RouteFilter.TOUS)
                    },
                    label = { Text("Tous") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == RouteFilter.LIKES,
                    onClick = { 
                        viewModel.setFilter(RouteFilter.LIKES)
                        // Refresh liked routes when switching to Likes tab
                        viewModel.loadLikedRoutes(currentUserId)
                    },
                    label = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFE91E63)
                            )
                            Text("Likes")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
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
            } else if (filteredRoutes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedFilter) {
                                RouteFilter.TOUS -> "Aucun parcours dans le cache"
                                RouteFilter.LIKES -> "Aucun parcours liké"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (selectedFilter) {
                                RouteFilter.TOUS -> "Générez des parcours pour les voir ici"
                                RouteFilter.LIKES -> "Likez des parcours pour les voir ici"
                            },
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    items(filteredRoutes) { route ->
                        // Get starting point from RouteSelectionViewModel if available
                        val startingPoint = routeSelectionVM?.startingPoint?.value
                        SavedRouteCard(
                            route = route,
                            startingPoint = startingPoint,
                            onViewDetails = {
                                navController.navigate("route_detail/${route.id}")
                            },
                            onToggleLike = {
                                viewModel.toggleLike(route.id, currentUserId)
                            },
                            onDelete = {
                                viewModel.deleteRoute(route.id, currentUserId)
                            },
                            onShare = {
                                shareRoute(context, route)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedRouteCard(
    route: RouteResponse,
    startingPoint: Pair<Double, Double>? = null,
    onViewDetails: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val routeTypeName = when (route.routeType) {
        "ECONOMIC" -> "Économique"
        "BALANCED" -> "Équilibré"
        "COMFORT" -> "Confort"
        else -> route.name
    }
    
    val transportationLabel = when (route.transportationMode ?: "MIXED") {
        "WALKING" -> "Marche"
        "BICYCLE" -> "Vélo"
        "PUBLIC_TRANSPORT" -> "Transport"
        "CAR" -> "Voiture"
        "MIXED" -> "Mixte"
        else -> route.transportationMode ?: "Mixte"
    }
    
    val budgetFormatted = "%.0f€".format(route.totalBudget)
    val stepsCount = route.steps.size
    
    // Calculate total distance from step distances
    val totalDistance = route.steps.sumOf { it.distanceFromPrevious ?: 0.0 }
    
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
            // Header with title
            Text(
                text = routeTypeName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            // Mini map with actual route data and buttons overlaid on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Map as background
                if (route.steps.isNotEmpty() || startingPoint != null) {
                    // Build markers: starting point first (if available), then steps
                    val markers = mutableListOf<com.tino.travelpath.ui.components.MapMarker>()
                    
                    // Add starting point as first marker (distinct)
                    startingPoint?.let { (lat, lng) ->
                        markers.add(
                            com.tino.travelpath.ui.components.MapMarker(
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
                            com.tino.travelpath.ui.components.MapMarker(
                                latitude = step.place.latitude,
                                longitude = step.place.longitude,
                                title = step.place.name,
                                isStartingPoint = false
                            )
                        )
                    }
                    
                    // Build polyline: starting point first (if available), then steps
                    val routePolylinePoints = mutableListOf<org.osmdroid.util.GeoPoint>()
                    startingPoint?.let { (lat, lng) ->
                        routePolylinePoints.add(org.osmdroid.util.GeoPoint(lat, lng))
                    }
                    route.steps.forEach { step ->
                        routePolylinePoints.add(
                            org.osmdroid.util.GeoPoint(step.place.latitude, step.place.longitude)
                        )
                    }
                    
                    // Center map on starting point if available, otherwise first step
                    val centerLat = startingPoint?.first ?: route.steps.firstOrNull()?.place?.latitude ?: 0.0
                    val centerLng = startingPoint?.second ?: route.steps.firstOrNull()?.place?.longitude ?: 0.0
                    
                    com.tino.travelpath.ui.components.OpenStreetMapView(
                        centerLatitude = centerLat,
                        centerLongitude = centerLng,
                        markers = markers,
                        routePolyline = routePolylinePoints.takeIf { it.isNotEmpty() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune étape disponible", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                
                // Buttons overlaid on top-right corner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.9f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (route.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (route.isFavorite == true) "Retirer des favoris" else "Ajouter aux favoris",
                            modifier = Modifier.size(20.dp),
                            tint = if (route.isFavorite == true) Color(0xFFE91E63) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.9f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(15.dp))
            
            // Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Budget
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
                    Text("$budgetFormatted", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                // Transportation modes - show icons based on step distances
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    getTransportationModeIcons(
                        mode = route.transportationMode ?: "MIXED",
                        selectedModes = emptySet(), // Saved routes don't have selected modes info
                        steps = route.steps
                    ).forEach { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF667eea)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "$stepsCount étapes",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    )
                ) {
                    Text("Voir détails", color = Color.White, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Partager",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF667eea)
                    )
                }
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
    selectedModes: Set<TransportationMode>,
    steps: List<com.tino.travelpath.data.api.dto.StepResponse>
): List<androidx.compose.ui.graphics.vector.ImageVector> {
    // If it's a single mode (not MIXED), show only that mode if it was selected
    val singleMode = when (mode.uppercase()) {
        "WALKING" -> {
            if (selectedModes.contains(TransportationMode.WALKING) || selectedModes.isEmpty()) {
                listOf(Icons.Default.DirectionsWalk)
            } else {
                emptyList()
            }
        }
        "BICYCLE" -> {
            if (selectedModes.contains(TransportationMode.BICYCLE) || selectedModes.isEmpty()) {
                listOf(Icons.Default.DirectionsBike)
            } else {
                emptyList()
            }
        }
        "PUBLIC_TRANSPORT" -> {
            if (selectedModes.contains(TransportationMode.PUBLIC_TRANSPORT) || selectedModes.isEmpty()) {
                listOf(Icons.Default.Train)
            } else {
                emptyList()
            }
        }
        "CAR" -> {
            if (selectedModes.contains(TransportationMode.CAR) || selectedModes.isEmpty()) {
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
                TransportationMode.WALKING,
                TransportationMode.BICYCLE,
                TransportationMode.PUBLIC_TRANSPORT,
                TransportationMode.CAR
            )
        } else {
            selectedModes
        }
        
        // Check each selected mode against step distances
        modesToCheck.forEach { selectedMode ->
            val isRelevant = when (selectedMode) {
                TransportationMode.WALKING -> {
                    // Walking is relevant for short distances (< 2km)
                    stepDistances.isEmpty() || stepDistances.any { it < 2.0 } || stepDistances.all { it < 2.0 }
                }
                TransportationMode.BICYCLE -> {
                    // Bicycle is relevant for medium distances (1-5km)
                    stepDistances.isEmpty() || stepDistances.any { it in 1.0..5.0 }
                }
                TransportationMode.PUBLIC_TRANSPORT -> {
                    // Public transport is relevant for medium-long distances (2-10km)
                    stepDistances.isEmpty() || stepDistances.any { it in 2.0..10.0 }
                }
                TransportationMode.CAR -> {
                    // Car is relevant for long distances (> 5km)
                    stepDistances.isEmpty() || stepDistances.any { it > 5.0 }
                }
                else -> false
            }
            
            if (isRelevant) {
                when (selectedMode) {
                    TransportationMode.WALKING -> relevantModes.add(Icons.Default.DirectionsWalk)
                    TransportationMode.BICYCLE -> relevantModes.add(Icons.Default.DirectionsBike)
                    TransportationMode.PUBLIC_TRANSPORT -> relevantModes.add(Icons.Default.Train)
                    TransportationMode.CAR -> relevantModes.add(Icons.Default.DirectionsCar)
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
