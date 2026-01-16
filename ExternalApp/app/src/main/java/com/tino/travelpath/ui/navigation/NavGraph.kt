package com.tino.travelpath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tino.travelpath.ui.screens.*

@Composable
fun TravelPathNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable("${Screen.SignIn.route}?mode={mode}") { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "signup"
            SignInScreen(
                navController = navController,
                initialMode = if (mode == "login") SignInMode.LOGIN else SignInMode.SIGNUP
            )
        }
        composable(Screen.SignIn.route) {
            SignInScreen(
                navController = navController,
                initialMode = SignInMode.SIGNUP
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable("${Screen.Preferences.route}?city={city}") { backStackEntry ->
            val city = backStackEntry.arguments?.getString("city")
            PreferencesScreen(
                navController = navController,
                initialCityName = if (city != null) java.net.URLDecoder.decode(city, "UTF-8") else null
            )
        }
        composable(Screen.Preferences.route) {
            PreferencesScreen(navController = navController, initialCityName = null)
        }
        composable(Screen.Loading.route) {
            LoadingScreen(navController = navController)
        }
        composable(Screen.RouteSelection.route) {
            RouteSelectionScreen(navController = navController)
        }
        composable("${Screen.RouteDetail.route}/{parcoursId}") { backStackEntry ->
            val parcoursId = backStackEntry.arguments?.getString("parcoursId") ?: ""
            RouteDetailScreen(navController = navController, parcoursId = parcoursId)
        }
        composable("${Screen.StepDetail.route}/{routeId}/{stepOrder}") { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            val stepOrder = backStackEntry.arguments?.getString("stepOrder")?.toIntOrNull() ?: 1
            StepDetailScreen(navController = navController, routeId = routeId, stepOrder = stepOrder)
        }
        composable("${Screen.ActiveNavigation.route}?routeId={routeId}") { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            ActiveNavigationScreen(navController = navController, routeId = routeId)
        }
        composable(Screen.SavedRoutes.route) {
            SavedRoutesScreen(
                navController = navController,
                routeSelectionViewModel = null // Will be loaded from ViewModelStoreOwner
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SignIn : Screen("sign_in")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Preferences : Screen("preferences")
    object Loading : Screen("loading")
    object RouteSelection : Screen("route_selection")
    object RouteDetail : Screen("route_detail")
    object StepDetail : Screen("step_detail")
    object ActiveNavigation : Screen("active_navigation")
    object SavedRoutes : Screen("saved_routes")
    object Profile : Screen("profile")
}
