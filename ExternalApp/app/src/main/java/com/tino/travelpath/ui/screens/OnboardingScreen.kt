package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tino.travelpath.R

@Composable
fun OnboardingScreen(navController: NavController) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        OnboardingPage(
            icon = "🗺️",
            title = stringResource(id = R.string.onboarding_page1_title),
            description = stringResource(id = R.string.onboarding_page1_description)
        ),
        OnboardingPage(
            icon = "⚙️",
            title = stringResource(id = R.string.onboarding_page2_title),
            description = stringResource(id = R.string.onboarding_page2_description)
        ),
        OnboardingPage(
            icon = "🧭",
            title = stringResource(id = R.string.onboarding_page3_title),
            description = stringResource(id = R.string.onboarding_page3_description)
        )
    )
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Text("×", fontSize = 24.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pages[currentPage].icon,
                    fontSize = 60.sp
                )
            }
            
            Text(
                text = pages[currentPage].title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 15.dp)
            )
            
            Text(
                text = pages[currentPage].description,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 30.dp)
            )
            
            Row(
                modifier = Modifier.padding(vertical = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (index == currentPage) Color(0xFF667eea) else Color.LightGray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
            
            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        // After onboarding, go to sign up
                        navController.navigate("sign_in?mode=signup")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text(
                    text = if (currentPage < pages.size - 1) stringResource(id = R.string.onboarding_next_button) else stringResource(id = R.string.onboarding_start_button),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

data class OnboardingPage(
    val icon: String,
    val title: String,
    val description: String
)

