package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.tino.travelpath.R
import com.tino.travelpath.data.database.TravelPathDatabase
import com.tino.travelpath.data.repository.ProfileRepository
import com.tino.travelpath.data.repository.UserRepository
import com.tino.travelpath.ui.viewmodels.AuthViewModel
import com.tino.travelpath.ui.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            userRepository = UserRepository(
                TravelPathDatabase.getDatabase(
                    androidx.compose.ui.platform.LocalContext.current
                ).utilisateurDao()
            ),
            profileRepository = ProfileRepository(
                TravelPathDatabase.getDatabase(
                    androidx.compose.ui.platform.LocalContext.current
                ).profilDao(),
                TravelPathDatabase.getDatabase(
                    androidx.compose.ui.platform.LocalContext.current
                ).preferencesDao()
            )
        )
    )
) {
    // Handle system back button
    BackHandler {
        navController.popBackStack()
    }
    
    val currentUser by viewModel.currentUser.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load current user on startup if not already loaded
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            val database = TravelPathDatabase.getDatabase(context)
            val usersFlow = database.utilisateurDao().getAllFlow()
            val users = usersFlow.firstOrNull()
            users?.firstOrNull()?.let { user ->
                viewModel.loadCurrentUserByEmail(user.email)
            }
        }
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(stringResource(id = R.string.profile_back_button))
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = "profile")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            item {
                // Profile header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF667eea), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.nom?.take(1)?.uppercase() ?: "?",
                            fontSize = 40.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(15.dp))
                    
                    Text(
                        text = currentUser?.nom ?: stringResource(id = R.string.profile_username),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = currentUser?.email ?: stringResource(id = R.string.profile_email),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    if (currentUser == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Non connecté",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { navController.navigate("sign_in") }
                        ) {
                            Text("Se connecter")
                        }
                    }
                }
                
                // Profils section
                Text(
                    text = stringResource(id = R.string.profile_profiles_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                
                ProfileItem(stringResource(id = R.string.profile_family_profile))
                ProfileItem(stringResource(id = R.string.profile_seniors_profile))
                ProfileItem(stringResource(id = R.string.profile_create_profile_button))
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Préférences par défaut
                Text(
                    text = stringResource(id = R.string.profile_default_preferences_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Text(stringResource(id = R.string.profile_edit_button))
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Paramètres
                Text(
                    text = stringResource(id = R.string.profile_settings_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                
                ProfileItem(stringResource(id = R.string.profile_language_setting))
                ProfileItem(stringResource(id = R.string.profile_units_setting))
                ProfileItem(stringResource(id = R.string.profile_notifications_setting))
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Données
                Text(
                    text = stringResource(id = R.string.profile_data_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                
                ProfileItem(stringResource(id = R.string.profile_offline_mode_setting))
                ProfileItem(stringResource(id = R.string.profile_clear_cache_setting))
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Aide
                Text(
                    text = stringResource(id = R.string.profile_help_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                
                ProfileItem(stringResource(id = R.string.profile_about_setting))
                ProfileItem(stringResource(id = R.string.profile_contact_setting))
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Déconnexion
                if (currentUser != null) {
                    Button(
                        onClick = {
                            viewModel.signOut()
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "Se déconnecter",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileItem(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontSize = 16.sp)
        }
    }
}

