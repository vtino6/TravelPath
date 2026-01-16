package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tino.travelpath.R
import com.tino.travelpath.data.model.Photo
import com.tino.travelpath.ui.viewmodels.CitySearchViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: CitySearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
    var isExploreMode by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExploreMode) {
                    // Back button when in explore mode
                    IconButton(onClick = { 
                        isExploreMode = false
                        viewModel.clearSelection()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Retour",
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = "Explorer des photos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                } else {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(id = R.string.home_profile_button_description))
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = "home")
        }
    ) { padding ->
        if (isExploreMode) {
            // Explore Mode: Show search and photos
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                // City Search Bar
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { 
                                    Text(
                                        text = "Rechercher une ville...",
                                        color = Color.Gray
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (selectedCity != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { viewModel.clearSelection() }
                                ) {
                                    Text("✕", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    
                    // City Suggestions Dropdown
                    // Show suggestions when there are cities and either no city is selected OR user is typing
                    if (cities.isNotEmpty() && (selectedCity == null || searchQuery != selectedCity)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 70.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                items(cities) { city ->
                                    TextButton(
                                        onClick = { viewModel.selectCity(city) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = city,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                        )
                                    }
                                    Divider()
                                }
                            }
                        }
                    }
                }
            
            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Error Message
            error?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFD32F2F)
                    )
                }
            }
            
            // Photos Gallery
            if (selectedCity != null && photos.isNotEmpty()) {
                Text(
                    text = "Photos de $selectedCity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(photos) { photo ->
                        PhotoCard(
                            photo = photo,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }
            } else if (selectedCity != null && photos.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune photo trouvée pour $selectedCity",
                        color = Color.Gray
                    )
                }
            }
            }
        } else {
            // Hero Section: Show two action buttons
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Welcome message
                Text(
                    text = "Bienvenue !",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF667eea),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Que souhaitez-vous faire ?",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Two action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Explore Photos Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clickable { isExploreMode = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4A90E2)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(bottom = 10.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Découvrir",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "des lieux",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Text(
                                text = "Explorez les lieux et\nleurs photos de la\ncommunauté",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Create Itinerary Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clickable { navController.navigate("preferences") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF667eea)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(bottom = 10.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Créer",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "un parcours",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Text(
                                text = "Générez votre\nitinéraire\npersonnalisé",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Full-screen photo viewer dialog
        selectedPhoto?.let { photo ->
            FullScreenPhotoViewer(
                photo = photo,
                onDismiss = { selectedPhoto = null }
            )
        }
    }
}

@Composable
fun FullScreenPhotoViewer(
    photo: Photo,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Full-screen image
            AsyncImage(
                model = photo.getImageUrlOrUrl(),
                contentDescription = photo.title ?: photo.locationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Photo info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                photo.locationName?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                photo.getAuthorNameOrUserName()?.let {
                    Text(
                        text = "Par $it",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
                photo.title?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = photo.getImageUrlOrUrl(),
                contentDescription = photo.title ?: photo.locationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            // Photo info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                photo.locationName?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                photo.getAuthorNameOrUserName()?.let {
                    Text(
                        text = "Par $it",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String) {
    val items = listOf(
        BottomNavItem(Icons.Default.Home, stringResource(id = R.string.bottom_nav_home), "home"),
        BottomNavItem(Icons.Default.LocationOn, stringResource(id = R.string.bottom_nav_itineraries), "saved_routes"),
        BottomNavItem(Icons.Default.Person, stringResource(id = R.string.bottom_nav_profile), "profile")
    )
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label, fontSize = 12.sp) },
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

data class BottomNavItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val route: String
)


