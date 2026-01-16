package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import com.tino.travelpath.ui.utils.ImageUtils
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * StepDetailScreen - Display details of a specific step from a route
 * Uses real data from backend via RouteSelectionViewModel
 */
@Composable
fun StepDetailScreen(navController: NavController, routeId: String, stepOrder: Int) {
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
    
    // Get route from ViewModel
    val viewModel: RouteSelectionViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "route_selection",
        factory = RouteSelectionViewModelFactory(RoutesRepository())
    )
    
    val routes by viewModel.routes.collectAsState()
    var route by remember { mutableStateOf<com.tino.travelpath.data.api.dto.RouteResponse?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Find route and step
    LaunchedEffect(routeId, routes) {
        val foundRoute = routes.find { it.id == routeId }
        if (foundRoute != null) {
            route = foundRoute
        } else {
            try {
                val loadedRoute = RoutesRepository().getRouteById(routeId)
                route = loadedRoute
            } catch (e: Exception) {
                android.util.Log.e("StepDetailScreen", "Error loading route", e)
            }
        }
    }
    
    val step = route?.steps?.find { it.order == stepOrder }
    
    if (route == null || step == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    val place = step.place
    val categoryLabel = when (place.category) {
        "RESTAURANT" -> "Restauration"
        "LEISURE" -> "Loisirs"
        "CULTURE" -> "Culture"
        "DISCOVERY" -> "Découverte"
        else -> place.category
    }
    
    val costText = if (step.cost != null && step.cost > 0) {
        "%.2f€".format(step.cost)
    } else {
        "Gratuit"
    }
    
    val durationText = "${step.estimatedDuration}min"
    val distanceText = if (step.distanceFromPrevious != null) {
        "${String.format("%.2f", step.distanceFromPrevious)}km"
    } else {
        "Distance non disponible"
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("← Retour")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero image using category-based image
            AsyncImage(
                model = ImageUtils.getImageUrlForCategory(place.category, place.id),
                contentDescription = "Image de ${place.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFFE0E0E0)),
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFFF5F5F5))
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = place.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            InfoRow("🏛️", categoryLabel)
            if (place.address != null) {
                InfoRow("📍", place.address)
            }
            InfoRow("💰", "Coût: $costText")
            InfoRow("⏱️", "Durée: $durationText")
            if (step.distanceFromPrevious != null) {
                InfoRow("📏", "Distance: $distanceText")
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (place.description != null && place.description.isNotBlank()) {
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                Text(
                    text = place.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    coroutineScope.launch {
                        try {
                            val routeToSave = routes.find { it.id == routeId } ?: route
                            if (routeToSave != null) {
                                android.util.Log.d("StepDetailScreen", "Saving route before starting navigation: $routeId")
                                viewModel.saveRoute(routeToSave)
                            }
                            navController.navigate("active_navigation?routeId=$routeId")
                        } catch (e: Exception) {
                            android.util.Log.e("StepDetailScreen", "Error saving route before navigation", e)
                            navController.navigate("active_navigation?routeId=$routeId")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text("🗺️ Commencer Navigation", modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
fun InfoRow(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(text, fontSize = 16.sp)
    }
}


