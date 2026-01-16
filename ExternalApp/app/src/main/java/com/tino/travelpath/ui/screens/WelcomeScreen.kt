package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tino.travelpath.R

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF667eea),
                        androidx.compose.ui.graphics.Color(0xFF764ba2)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🗺️ ${stringResource(id = R.string.app_name)}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            Text(
                text = stringResource(id = R.string.welcome_subtitle),
                fontSize = 18.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 60.dp)
            )
            
            Button(
                onClick = { navController.navigate("onboarding") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF667eea)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_start_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            TextButton(
                onClick = { navController.navigate("sign_in?mode=login") },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_login_button),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

