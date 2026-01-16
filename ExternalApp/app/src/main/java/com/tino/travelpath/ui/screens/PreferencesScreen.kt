package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tino.travelpath.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tino.travelpath.ui.utils.LocationHelper
import androidx.compose.material.icons.filled.LocationOn
import android.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import java.net.URLEncoder
import com.tino.travelpath.data.database.TravelPathDatabase
import com.tino.travelpath.data.database.entities.Activite
import com.tino.travelpath.data.database.entities.TransportationMode
import com.tino.travelpath.data.repository.PlacesRepository
import com.tino.travelpath.ui.viewmodels.PreferencesViewModel
import com.tino.travelpath.ui.viewmodels.PreferencesViewModelFactory

@Composable
fun PreferencesScreen(
    navController: NavController,
    initialCityName: String? = null
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val localViewModelStoreOwner = LocalViewModelStoreOwner.current
    
    android.util.Log.d("PreferencesScreen", "Activity: $activity")
    android.util.Log.d("PreferencesScreen", "Local ViewModelStoreOwner: $localViewModelStoreOwner")
    
    val viewModelStoreOwner = activity ?: localViewModelStoreOwner
    
    if (viewModelStoreOwner == null) {
        android.util.Log.e("PreferencesScreen", "ERROR: No ViewModelStoreOwner available!")
        return
    }
    val viewModel: PreferencesViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "preferences",
        factory = PreferencesViewModelFactory(
            PlacesRepository(
                TravelPathDatabase.getDatabase(context).lieuDao()
            )
        )
    )
    
    android.util.Log.d("PreferencesScreen", "Got PreferencesViewModel: $viewModel")
    android.util.Log.d("PreferencesScreen", "ViewModel hash: ${viewModel.hashCode()}")
    val selectedActivities by viewModel.selectedActivities.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val numberOfPlaces by viewModel.numberOfPlaces.collectAsState()
    val selectedTransportationModes by viewModel.selectedTransportationModes.collectAsState()
    val location by viewModel.location.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(initialCityName) {
        if (initialCityName != null && initialCityName.isNotBlank()) {
            coroutineScope.launch {
                try {
                    val coords = geocodeLocation(initialCityName)
                    if (coords != null) {
                        viewModel.setLocation(coords.first, coords.second, initialCityName)
                        android.util.Log.d("PreferencesScreen", "Geocoded initial city: $initialCityName -> (${coords.first}, ${coords.second})")
                    } else {
                        android.util.Log.w("PreferencesScreen", "Could not geocode initial city: $initialCityName")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PreferencesScreen", "Error geocoding initial city", e)
                }
            }
        }
    }
    
    val isLocationSet = location != null
    
    val activities = listOf(
        stringResource(id = R.string.preferences_activity_restaurant) to Activite.RESTAURATION,
        stringResource(id = R.string.preferences_activity_leisure) to Activite.LOISIRS,
        stringResource(id = R.string.preferences_activity_discovery) to Activite.DECOUVERTE,
        stringResource(id = R.string.preferences_activity_culture) to Activite.CULTURE
    )
    
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(stringResource(id = R.string.preferences_title))
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
            // Starting Point Section (replaces the old Location section)
            SectionTitle("📍 Point de départ")
            Text(
                text = "L'itinéraire commencera depuis ce point",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val locationHelper = remember { LocationHelper(context) }
            val currentLocationName = locationName ?: ""
            var searchQuery by remember { mutableStateOf(currentLocationName) }
            var showSuggestions by remember { mutableStateOf(false) }
            var suggestions by remember { mutableStateOf<List<Pair<String, Pair<Double, Double>>>>(emptyList()) }
            var isSearching by remember { mutableStateOf(false) }
            var locationPermissionGranted by remember { 
                mutableStateOf(
                    try {
                        locationHelper.hasLocationPermission()
                    } catch (e: Exception) {
                        false
                    }
                )
            }
            
            // Sync searchQuery with locationName when location changes (but only if user isn't actively typing)
            LaunchedEffect(currentLocationName) {
                if (currentLocationName.isNotEmpty() && searchQuery != currentLocationName && !showSuggestions && !isSearching) {
                    searchQuery = currentLocationName
                }
            }
            
            // Debounced search with LaunchedEffect (like Google Maps)
            LaunchedEffect(searchQuery) {
                // Capture the current query value
                val queryToSearch = searchQuery
                
                // Don't search if query matches current location name (user selected a location)
                if (queryToSearch == currentLocationName && currentLocationName.isNotEmpty()) {
                    showSuggestions = false
                    suggestions = emptyList()
                    isSearching = false
                    return@LaunchedEffect
                }
                
                if (queryToSearch.length >= 2) {
                    // Debounce: wait 300ms after user stops typing
                    kotlinx.coroutines.delay(300)
                    
                    // Check if query hasn't changed during the delay
                    if (searchQuery == queryToSearch && queryToSearch != currentLocationName) {
                        showSuggestions = true
                        isSearching = true
                        android.util.Log.d("PreferencesScreen", "Searching for: '$queryToSearch'")
                        val results = try {
                            searchLocationSuggestions(queryToSearch)
                        } catch (e: Exception) {
                            android.util.Log.e("PreferencesScreen", "Error searching suggestions", e)
                            e.printStackTrace()
                            emptyList()
                        }
                        // Only update if query hasn't changed during the search
                        if (searchQuery == queryToSearch) {
                            suggestions = results
                            showSuggestions = results.isNotEmpty() || true // Always show dropdown
                            isSearching = false
                            android.util.Log.d("PreferencesScreen", "Found ${results.size} suggestions for '$queryToSearch'")
                        } else {
                            android.util.Log.d("PreferencesScreen", "Query changed during search, ignoring results")
                            if (searchQuery == queryToSearch) {
                                isSearching = false
                            }
                        }
                    } else {
                        android.util.Log.d("PreferencesScreen", "Query changed during debounce, cancelling search")
                        showSuggestions = false
                        suggestions = emptyList()
                        isSearching = false
                    }
                } else {
                    showSuggestions = false
                    suggestions = emptyList()
                    isSearching = false
                }
            }
            
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            }
            
            // Search field with GPS button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        // LaunchedEffect will handle the search with debouncing
                    },
                    label = { Text("Rechercher une ville, adresse ou lieu") },
                    placeholder = { Text("Ex: Paris, Eiffel Tower, 123 Main St") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (currentLocationName.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.clearLocation()
                                    searchQuery = ""
                                    showSuggestions = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Effacer",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                
                // GPS button
                Button(
                    onClick = {
                        if (locationPermissionGranted) {
                            coroutineScope.launch {
                                val gpsLocation = locationHelper.getCurrentLocation()
                                if (gpsLocation != null) {
                                    // Reverse geocode to get actual address
                                    val address = reverseGeocode(
                                        gpsLocation.latitude,
                                        gpsLocation.longitude
                                    ) ?: "Ma position GPS"
                                    
                                    viewModel.setLocation(
                                        gpsLocation.latitude,
                                        gpsLocation.longitude,
                                        address
                                    )
                                    searchQuery = address
                                    showSuggestions = false
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    ),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Utiliser ma position",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Show selected location if set (with clear button)
            val currentLocation: Pair<Double, Double>? = location
            if (currentLocation != null && currentLocationName.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF667eea),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Point de départ sélectionné",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = currentLocationName,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.clearLocation()
                                searchQuery = ""
                                showSuggestions = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Suggestions dropdown (show when searching or when there are results or when search completed with no results)
            if (showSuggestions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        if (isSearching && suggestions.isEmpty()) {
                            // Show loading state
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Recherche en cours...",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else if (suggestions.isEmpty() && !isSearching) {
                            // No results found
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aucun résultat trouvé",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            // Show suggestions
                            items(
                                items = suggestions,
                                key = { it.first } // Use name as key
                            ) { (name, coords) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setLocation(coords.first, coords.second, name)
                                            searchQuery = name
                                            showSuggestions = false
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF667eea),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
            
            SectionTitle("Contrainte: ${stringResource(id = R.string.preferences_activities_section_title)}")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activities.forEach { (label, activity) ->
                    val isSelected = selectedActivities.contains(activity)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            android.util.Log.d("PreferencesScreen", "Toggling activity: $activity, currently selected: $isSelected")
                            viewModel.toggleActivity(activity)
                            android.util.Log.d("PreferencesScreen", "After toggle, selected activities: ${viewModel.selectedActivities.value}")
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(vertical = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isSelected) Color(0xFF667eea) else Color(0xFFF5F5F5),
                            labelColor = if (isSelected) Color.White else Color.Black,
                            selectedContainerColor = Color(0xFF667eea),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            if (selectedActivities.isNotEmpty()) {
                Text(
                    text = "${selectedActivities.size} activité(s) sélectionnée(s)",
                    fontSize = 12.sp,
                    color = Color(0xFF667eea),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            
            SectionTitle("Contrainte: ${stringResource(id = R.string.preferences_budget_section_title)}")
            var budgetText by remember { mutableStateOf(budget.toInt().toString()) }
            
            LaunchedEffect(budget) {
                budgetText = budget.toInt().toString()
            }
            
            OutlinedTextField(
                value = budgetText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        budgetText = newValue
                        if (newValue.isNotEmpty()) {
                            try {
                                val budgetValue = newValue.toFloat().coerceIn(0f, 1000f)
                                viewModel.setBudget(budgetValue)
                            } catch (e: NumberFormatException) {
                            }
                        }
                    }
                },
                label = { Text("Budget maximum (${stringResource(id = R.string.preferences_currency_symbol)})") },
                placeholder = { Text("Ex: 50") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                suffix = { Text(stringResource(id = R.string.preferences_currency_symbol)) }
            )
            Text(
                text = "Le nombre de lieux sera calculé automatiquement",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 15.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            SectionTitle("Contrainte: Nombre de lieux")
            var numberOfPlacesText by remember { mutableStateOf(numberOfPlaces.toString()) }
            
            LaunchedEffect(numberOfPlaces) {
                numberOfPlacesText = numberOfPlaces.toString()
            }
            
            OutlinedTextField(
                value = numberOfPlacesText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        numberOfPlacesText = newValue
                        if (newValue.isNotEmpty()) {
                            try {
                                val placesValue = newValue.toInt().coerceAtLeast(1)
                                viewModel.setNumberOfPlaces(placesValue)
                            } catch (e: NumberFormatException) {
                                // Invalid number, keep text but don't update value
                            }
                        } else {
                            // Empty field, set to default 1
                            viewModel.setNumberOfPlaces(1)
                        }
                    }
                },
                label = { Text("Nombre de lieux à visiter") },
                placeholder = { Text("Ex: 5") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                suffix = { Text("lieux") }
            )
            
            SectionTitle("Mode de transport")
            
            // Single row with 4 transportation modes (excluding MIXED)
            val modes = listOf(
                TransportationMode.WALKING,
                TransportationMode.BICYCLE,
                TransportationMode.PUBLIC_TRANSPORT,
                TransportationMode.CAR
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { mode ->
                    val isSelected = selectedTransportationModes.contains(mode)
                    
                    // Icon for each mode
                    val icon = when (mode) {
                        TransportationMode.WALKING -> Icons.Default.DirectionsWalk
                        TransportationMode.BICYCLE -> Icons.Default.DirectionsBike
                        TransportationMode.PUBLIC_TRANSPORT -> Icons.Default.Train
                        TransportationMode.CAR -> Icons.Default.DirectionsCar
                        else -> Icons.Default.DirectionsWalk // Fallback (should not happen)
                    }
                    
                    // Smaller card with icon
                    Card(
                        onClick = { viewModel.toggleTransportationMode(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .height(70.dp), // Smaller height to fit on one line
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                Color(0xFF667eea).copy(alpha = 0.1f) 
                            else 
                                Color(0xFFF5F5F5)
                        ),
                        border = if (isSelected) {
                            BorderStroke(
                                2.dp, 
                                Color(0xFF667eea)
                            )
                        } else null,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 4.dp else 2.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = when (mode) {
                                    TransportationMode.WALKING -> "Marche"
                                    TransportationMode.BICYCLE -> "Vélo"
                                    TransportationMode.PUBLIC_TRANSPORT -> "Transport public"
                                    TransportationMode.CAR -> "Voiture"
                                    else -> ""
                                },
                                modifier = Modifier.size(32.dp), // Smaller icon
                                tint = if (isSelected) 
                                    Color(0xFF667eea) 
                                else 
                                    Color.Gray
                            )
                        }
                    }
                }
            }
            
            var showValidationError by remember { mutableStateOf(false) }
            var showBudgetWarning by remember { mutableStateOf(false) }
            var budgetWarningData by remember { mutableStateOf<BudgetWarningData?>(null) }
            
            if (showValidationError) {
                Column {
                    if (selectedActivities.isEmpty()) {
                        Text(
                            text = "⚠️ Veuillez sélectionner au moins une activité",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (!isLocationSet) {
                        Text(
                            text = "⚠️ Veuillez sélectionner une localisation",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            
            Button(
                onClick = { 
                    android.util.Log.d("PreferencesScreen", "Generate button clicked")
                    android.util.Log.d("PreferencesScreen", "Selected activities: ${selectedActivities.size}")
                    android.util.Log.d("PreferencesScreen", "Activities: $selectedActivities")
                    
                    if (selectedActivities.isEmpty()) {
                        android.util.Log.w("PreferencesScreen", "Cannot generate: No activities selected")
                        showValidationError = true
                        return@Button
                    }
                    
                    if (!isLocationSet) {
                        android.util.Log.w("PreferencesScreen", "Cannot generate: No location selected")
                        showValidationError = true
                        return@Button
                    }
                    
                    showValidationError = false
                    
                    android.util.Log.d("PreferencesScreen", "Validating before navigation:")
                    android.util.Log.d("PreferencesScreen", "  Activities: ${selectedActivities.size}")
                    android.util.Log.d("PreferencesScreen", "  Budget: $budget")
                    android.util.Log.d("PreferencesScreen", "  Number of Places: $numberOfPlaces")
                    android.util.Log.d("PreferencesScreen", "  Location: $location")
                    
                    // Pre-validation: Estimate cost
                    val costEstimate = viewModel.estimateCost(numberOfPlaces)
                    val estimatedCost = costEstimate.grandTotal // Include transportation
                    val userBudget = budget.toDouble()
                    
                    android.util.Log.d("PreferencesScreen", "Cost estimate: $estimatedCost€ (places: ${costEstimate.totalCost}€, transport: ${costEstimate.transportationCost}€), Budget: $userBudget€")
                    
                    // Check if budget is insufficient
                    if (estimatedCost > userBudget) {
                        // Show warning dialog
                        showBudgetWarning = true
                        budgetWarningData = BudgetWarningData(
                            requestedPlaces = numberOfPlaces,
                            currentBudget = userBudget,
                            estimatedCost = estimatedCost,
                            breakdown = costEstimate.breakdown,
                            transportationCost = costEstimate.transportationCost
                        )
                        return@Button
                    }
                    
                    // Budget is sufficient, proceed to generation
                    navController.navigate("loading") 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea),
                    disabledContainerColor = Color.Gray
                ),
                enabled = selectedActivities.isNotEmpty() && isLocationSet,
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.preferences_generate_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            
            if (selectedActivities.isEmpty()) {
                Text(
                    text = "Sélectionnez au moins une activité pour générer un itinéraire",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Budget Warning Dialog
            if (showBudgetWarning && budgetWarningData != null) {
                val data = budgetWarningData!!
                AlertDialog(
                    onDismissRequest = { showBudgetWarning = false },
                    title = {
                        Text(
                            text = "⚠️ Budget Insuffisant",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Votre budget (${String.format("%.0f", data.currentBudget)}€) est insuffisant pour ${data.requestedPlaces} lieux.",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "Coût estimé: ${String.format("%.0f", data.estimatedCost)}€",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF667eea),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "Répartition estimée:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            data.breakdown.forEach { (category, cost) ->
                                val categoryName = when (category) {
                                    "RESTAURANT" -> "🍽️ Restaurants"
                                    "CULTURE" -> "🎨 Culture"
                                    "LEISURE" -> "🎯 Loisirs"
                                    "DISCOVERY" -> "🔍 Découverte"
                                    else -> category
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = categoryName,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${String.format("%.0f", cost)}€",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            // Show transportation cost if > 0
                            if (data.transportationCost > 0) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🚇 Transport",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${String.format("%.2f", data.transportationCost)}€",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            Text(
                                text = "Options:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "• Augmenter le budget à ${String.format("%.0f", data.estimatedCost)}€",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                            Text(
                                text = "• Réduire le nombre de lieux à ${(data.requestedPlaces * (data.currentBudget / data.estimatedCost)).toInt().coerceAtLeast(1)}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                            Text(
                                text = "• Choisir des activités moins chères",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Quick fix: Increase budget
                                    viewModel.setBudget(data.estimatedCost.toFloat())
                                    showBudgetWarning = false
                                }
                            ) {
                                Text("Augmenter budget", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { showBudgetWarning = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF667eea)
                                )
                            ) {
                                Text("Ajuster manuellement", fontSize = 12.sp)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBudgetWarning = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }
}

data class BudgetWarningData(
    val requestedPlaces: Int,
    val currentBudget: Double,
    val estimatedCost: Double,
    val breakdown: Map<String, Double>,
    val transportationCost: Double = 0.0
)

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 15.dp)
    )
}

suspend fun searchLocationSuggestions(query: String): List<Pair<String, Pair<Double, Double>>> = withContext(Dispatchers.IO) {
    try {
        if (query.isBlank() || query.length < 2) {
            android.util.Log.d("Geocoding", "Query too short: '$query'")
            return@withContext emptyList()
        }
        
        // Use Google Places Autocomplete API (works exactly like Google Maps)
        val apiKey = "AIzaSyBZoWcf74F9ReDbJKzcIUpKT4AtzqH1eEE" // Google Places API key
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encodedQuery&key=$apiKey&language=fr,en&types=geocode"
        
        android.util.Log.d("Geocoding", "=== SEARCHING SUGGESTIONS (Google Places) ===")
        android.util.Log.d("Geocoding", "Query: '$query'")
        android.util.Log.d("Geocoding", "URL: ${url.replace(apiKey, "API_KEY")}")
        
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val responseCode = connection.responseCode
        android.util.Log.d("Geocoding", "Suggestions response code: $responseCode")
        
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("Geocoding", "Raw response (first 500 chars): ${response.take(500)}")
            
            if (response.isBlank()) {
                android.util.Log.w("Geocoding", "Empty suggestions response for '$query'")
                return@withContext emptyList()
            }
            
            try {
                val jsonObject = org.json.JSONObject(response)
                val status = jsonObject.getString("status")
                
                if (status != "OK" && status != "ZERO_RESULTS") {
                    android.util.Log.e("Geocoding", "Google Places API error status: $status")
                    if (jsonObject.has("error_message")) {
                        android.util.Log.e("Geocoding", "Error message: ${jsonObject.getString("error_message")}")
                    }
                    return@withContext emptyList()
                }
                
                val predictions = jsonObject.getJSONArray("predictions")
                android.util.Log.d("Geocoding", "Found ${predictions.length()} suggestions")
                
                val results = mutableListOf<Pair<String, Pair<Double, Double>>>()
                
                // For autocomplete, we'll get coordinates when user selects a suggestion
                // For now, use geocoding API to get coordinates for the description
                // This is faster than making N Place Details calls
                for (i in 0 until kotlin.math.min(predictions.length(), 5)) { // Limit to 5 for speed
                    try {
                        val prediction = predictions.getJSONObject(i)
                        val description = prediction.getString("description")
                        val placeId = prediction.getString("place_id")
                        
                        android.util.Log.d("Geocoding", "Processing suggestion $i: $description")
                        
                        // Use Geocoding API to get coordinates from the description (faster than Place Details)
                        val geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=${URLEncoder.encode(description, "UTF-8")}&key=$apiKey"
                        val geocodeConnection = java.net.URL(geocodeUrl).openConnection() as java.net.HttpURLConnection
                        geocodeConnection.requestMethod = "GET"
                        geocodeConnection.setRequestProperty("Accept", "application/json")
                        geocodeConnection.connectTimeout = 8000
                        geocodeConnection.readTimeout = 8000
                        
                        val geocodeResponseCode = geocodeConnection.responseCode
                        if (geocodeResponseCode == 200) {
                            val geocodeResponse = geocodeConnection.inputStream.bufferedReader().use { it.readText() }
                            val geocodeJson = org.json.JSONObject(geocodeResponse)
                            
                            if (geocodeJson.getString("status") == "OK") {
                                val geocodeResults = geocodeJson.getJSONArray("results")
                                if (geocodeResults.length() > 0) {
                                    val firstResult = geocodeResults.getJSONObject(0)
                                    val geometry = firstResult.getJSONObject("geometry")
                                    val location = geometry.getJSONObject("location")
                                    val lat = location.getDouble("lat")
                                    val lng = location.getDouble("lng")
                                    
                                    results.add(Pair(description, Pair(lat, lng)))
                                    android.util.Log.d("Geocoding", "Suggestion $i: $description ($lat, $lng)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Geocoding", "Error processing suggestion $i", e)
                    }
                }
                
                android.util.Log.d("Geocoding", "Returning ${results.size} suggestions")
                return@withContext results
            } catch (e: org.json.JSONException) {
                android.util.Log.e("Geocoding", "JSON parsing error for '$query': ${e.message}")
                android.util.Log.e("Geocoding", "Response was: ${response.take(500)}")
                e.printStackTrace()
                return@withContext emptyList()
            }
        } else {
            val errorBody = try {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            } catch (e: Exception) {
                "Error reading error stream: ${e.message}"
            }
            android.util.Log.e("Geocoding", "HTTP error $responseCode for suggestions '$query': $errorBody")
        }
        
        emptyList()
    } catch (e: java.net.SocketTimeoutException) {
        android.util.Log.e("Geocoding", "Timeout searching suggestions for '$query'", e)
        emptyList()
    } catch (e: java.net.UnknownHostException) {
        android.util.Log.e("Geocoding", "Network error searching suggestions for '$query'", e)
        emptyList()
    } catch (e: Exception) {
        android.util.Log.e("Geocoding", "Error searching suggestions for '$query'", e)
        e.printStackTrace()
        emptyList()
    }
}

suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
    try {
        // Use Google Places Reverse Geocoding API
        val apiKey = "AIzaSyBZoWcf74F9ReDbJKzcIUpKT4AtzqH1eEE"
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey&language=fr,en"
        
        android.util.Log.d("Geocoding", "Reverse geocoding ($latitude, $longitude) using Google Places")
        
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            if (response.isBlank()) {
                return@withContext null
            }
            
            val jsonObject = org.json.JSONObject(response)
            val status = jsonObject.getString("status")
            
            if (status == "OK") {
                val results = jsonObject.getJSONArray("results")
                if (results.length() > 0) {
                    val firstResult = results.getJSONObject(0)
                    val formattedAddress = firstResult.getString("formatted_address")
                    android.util.Log.d("Geocoding", "Reverse geocoded address: $formattedAddress")
                    return@withContext formattedAddress
                }
            } else {
                android.util.Log.w("Geocoding", "Reverse geocoding status: $status")
            }
        }
        
        null
    } catch (e: Exception) {
        android.util.Log.e("Geocoding", "Error reverse geocoding ($latitude, $longitude)", e)
        null
    }
}

suspend fun geocodeLocation(locationName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    try {
        val encodedName = URLEncoder.encode(locationName, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedName&format=json&limit=1&addressdetails=1&accept-language=en,fr"
        
        android.util.Log.d("Geocoding", "Geocoding '$locationName' (worldwide) with URL: $url")
        
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "TravelPath/1.0")
        connection.setRequestProperty("Accept-Language", "en,fr")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        android.util.Log.d("Geocoding", "Response code: $responseCode")
        
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("Geocoding", "Response: $response")
            
            if (response.isBlank() || response == "[]") {
                android.util.Log.w("Geocoding", "Empty response for '$locationName'")
                return@withContext null
            }
            
            val jsonArray = org.json.JSONArray(response)
            android.util.Log.d("Geocoding", "Found ${jsonArray.length()} results")
            
            if (jsonArray.length() > 0) {
                val firstResult = jsonArray.getJSONObject(0)
                val lat = firstResult.getString("lat").toDouble()
                val lon = firstResult.getString("lon").toDouble()
                val displayName = firstResult.optString("display_name", locationName)
                android.util.Log.d("Geocoding", "Found coordinates for '$locationName': ($lat, $lon) - $displayName")
                return@withContext Pair(lat, lon)
            }
        } else {
            val errorBody = try {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            } catch (e: Exception) {
                "Error reading error stream: ${e.message}"
            }
            android.util.Log.e("Geocoding", "HTTP error $responseCode for '$locationName': $errorBody")
        }
        
        android.util.Log.w("Geocoding", "No results found for '$locationName'")
        null
    } catch (e: java.net.SocketTimeoutException) {
        android.util.Log.e("Geocoding", "Timeout geocoding '$locationName'", e)
        null
    } catch (e: java.net.UnknownHostException) {
        android.util.Log.e("Geocoding", "Network error geocoding '$locationName'", e)
        null
    } catch (e: Exception) {
        android.util.Log.e("Geocoding", "Error geocoding '$locationName'", e)
        e.printStackTrace()
        null
    }
}

