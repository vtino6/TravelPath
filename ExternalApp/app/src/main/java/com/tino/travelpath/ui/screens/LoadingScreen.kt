package com.tino.travelpath.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.navigation.NavController
import com.tino.travelpath.R
import com.tino.travelpath.data.api.RetrofitClient
import com.tino.travelpath.data.database.TravelPathDatabase
import com.tino.travelpath.data.repository.PlacesRepository
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.ui.viewmodels.PreferencesViewModel
import com.tino.travelpath.ui.viewmodels.PreferencesViewModelFactory
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun LoadingScreen(
    navController: NavController
) {
    // Handle system back button - allow canceling route generation
    BackHandler {
        navController.popBackStack()
    }
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val localViewModelStoreOwner = LocalViewModelStoreOwner.current
    
    val viewModelStoreOwner = remember(activity, localViewModelStoreOwner) {
        activity ?: localViewModelStoreOwner
    }
    
    if (viewModelStoreOwner == null) {
        return
    }
    
    val preferencesViewModel: PreferencesViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "preferences",
        factory = PreferencesViewModelFactory(
            PlacesRepository(
                TravelPathDatabase.getDatabase(context).lieuDao()
            )
        )
    )
    
    val routeSelectionViewModel: RouteSelectionViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "route_selection",
        factory = RouteSelectionViewModelFactory(RoutesRepository())
    )
    
    // Log once when screen is first composed
    LaunchedEffect(Unit) {
        android.util.Log.d("LoadingScreen", "Activity: $activity")
        android.util.Log.d("LoadingScreen", "Local ViewModelStoreOwner: $localViewModelStoreOwner")
        android.util.Log.d("LoadingScreen", "Got PreferencesViewModel: $preferencesViewModel")
        android.util.Log.d("LoadingScreen", "ViewModel hash: ${preferencesViewModel.hashCode()}")
    }
    
    val routes by routeSelectionViewModel.routes.collectAsState()
    val isLoading by routeSelectionViewModel.isLoading.collectAsState()
    val error by routeSelectionViewModel.error.collectAsState()
    
    var statusMessage by remember { mutableStateOf("Préparation de votre itinéraire...") }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    // Get current user ID for user-specific route generation
    val currentUserIdState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val database = TravelPathDatabase.getDatabase(context)
            val users = database.utilisateurDao().getAllFlow().firstOrNull()
            currentUserIdState.value = users?.firstOrNull()?.id
            android.util.Log.d("LoadingScreen", "Current user ID: ${currentUserIdState.value}")
        } catch (e: Exception) {
            android.util.Log.e("LoadingScreen", "Error getting current user", e)
        }
    }
    val currentUserId = currentUserIdState.value
    
    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("LoadingScreen", "=== STARTING ROUTE GENERATION ===")
            
            android.util.Log.d("LoadingScreen", "ViewModel selectedActivities: ${preferencesViewModel.selectedActivities.value}")
            android.util.Log.d("LoadingScreen", "ViewModel location: ${preferencesViewModel.location.value}")
            
            statusMessage = "Vérification de la connexion..."
            android.util.Log.d("LoadingScreen", "Testing backend connectivity...")
            try {
                val testResult = RetrofitClient.apiService.testBackend()
                android.util.Log.d("LoadingScreen", "Backend test successful: $testResult")
                statusMessage = "Connexion établie..."
            } catch (e: Exception) {
                android.util.Log.e("LoadingScreen", "Backend test FAILED", e)
                validationError = "Impossible de se connecter au serveur. Vérifiez que le backend est démarré."
                statusMessage = "Erreur de connexion"
                return@LaunchedEffect
            }
            
            delay(100)
            
            val request = preferencesViewModel.buildRouteRequest()
            
            android.util.Log.d("LoadingScreen", "Built RouteRequest:")
            android.util.Log.d("LoadingScreen", "  Activities: ${request.activities}")
            android.util.Log.d("LoadingScreen", "  Activities count: ${request.activities.size}")
            android.util.Log.d("LoadingScreen", "  Location: ${request.latitude}, ${request.longitude}")
            android.util.Log.d("LoadingScreen", "  Budget: ${request.maxBudget}")
            android.util.Log.d("LoadingScreen", "  Number of Places: ${request.numberOfPlaces}")
            
            if (request.activities.isEmpty()) {
                android.util.Log.w("LoadingScreen", "No activities selected!")
                android.util.Log.w("LoadingScreen", "ViewModel has ${preferencesViewModel.selectedActivities.value.size} activities")
                validationError = "Veuillez sélectionner au moins une activité. Retournez à l'écran précédent et sélectionnez des activités."
                return@LaunchedEffect
            }
            
            validationError = null
            statusMessage = "Recherche de lieux..."
            val locationName = preferencesViewModel.locationName.value
            val selectedModes = preferencesViewModel.selectedTransportationModes.value
            android.util.Log.d("LoadingScreen", "Calling routeSelectionViewModel.generateRoutes() with userId: $currentUserId, locationName: $locationName, selectedModes: $selectedModes")
            routeSelectionViewModel.generateRoutes(request, currentUserId, locationName, selectedModes)
            android.util.Log.d("LoadingScreen", "Route generation request sent")
            
            // Update status as generation progresses
            kotlinx.coroutines.delay(2000)
            if (isLoading) {
                statusMessage = "Calcul des distances..."
                android.util.Log.d("LoadingScreen", "Status: Calculating distances...")
            }
            kotlinx.coroutines.delay(2000)
            if (isLoading) {
                statusMessage = "Optimisation des itinéraires..."
                android.util.Log.d("LoadingScreen", "Status: Optimizing routes...")
            }
        } catch (e: Exception) {
            android.util.Log.e("LoadingScreen", "Error in LaunchedEffect", e)
            // Error will be shown in UI
        }
    }
    
    // Navigate to route selection when routes are generated
    LaunchedEffect(routes, isLoading, error) {
        android.util.Log.d("LoadingScreen", "=== NAVIGATION CHECK ===")
        android.util.Log.d("LoadingScreen", "Routes count: ${routes.size}")
        android.util.Log.d("LoadingScreen", "Is loading: $isLoading")
        android.util.Log.d("LoadingScreen", "Error: $error")
        
        if (!isLoading) {
            if (routes.isNotEmpty()) {
                // Success - navigate to route selection
                android.util.Log.d("LoadingScreen", "Navigating to route_selection with ${routes.size} routes")
                delay(500) // Small delay for smooth transition
                navController.navigate("route_selection") {
                    popUpTo("preferences") { inclusive = false }
                }
            } else if (error != null) {
                android.util.Log.e("LoadingScreen", "Error occurred: $error")
            } else {
                android.util.Log.w("LoadingScreen", "No routes, no error, loading complete - might be empty result")
            }
        } else {
            android.util.Log.d("LoadingScreen", "Still loading, waiting...")
        }
    }
    
    LaunchedEffect(Unit) {
        delay(30000)
        if (isLoading) {
            statusMessage = "Cela prend plus de temps que prévu..."
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .rotate(rotation)
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Color(0xFF667eea),
                strokeWidth = 4.dp
            )
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Text(
            text = stringResource(id = R.string.loading_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )
        
        Text(
            text = statusMessage,
            fontSize = 14.sp,
            color = Color(0xFF667eea),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        Text(
            text = stringResource(id = R.string.loading_tip),
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 30.dp)
        )
        
        val displayError = validationError ?: error
        displayError?.let {
            Text(
                text = "Erreur: $it",
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(stringResource(id = R.string.loading_cancel_button))
            }
            
            // Retry button if error
            if (displayError != null) {
                Button(
                    onClick = {
                    validationError = null
                    val request = preferencesViewModel.buildRouteRequest()
                    if (request.activities.isNotEmpty()) {
                        val locationName = preferencesViewModel.locationName.value
                        val selectedModes = preferencesViewModel.selectedTransportationModes.value
                        routeSelectionViewModel.generateRoutes(request, null, locationName, selectedModes)
                    } else {
                        validationError = "Veuillez sélectionner au moins une activité"
                    }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    )
                ) {
                    Text("Réessayer", color = Color.White)
                }
            }
        }
    }
}

