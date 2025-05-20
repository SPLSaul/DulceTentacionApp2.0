package com.example.ddma

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ddma.data.api.CarritoService
import com.example.ddma.di.DependencyProvider
import com.example.ddma.ui.screens.CarritoScreen
import com.example.ddma.ui.screens.LoginScreen
import com.example.ddma.ui.screens.PastelesScreen
import com.example.ddma.ui.theme.DdmaTheme

class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar dependencias
        DependencyProvider.init(applicationContext)

        // Obtener instancias de los repositorios y servicios
        val carritoService = DependencyProvider.carritoService
        val paymentRepository = DependencyProvider.paymentRepository
        val pastelesRepository = DependencyProvider.pastelesRepository

        setContent {
            DdmaTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { userId ->
                                if (userId > 0) {
                                    navController.navigate("pasteles/$userId") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = "pasteles/{userId}",
                        arguments = listOf(navArgument("userId") {
                            type = NavType.IntType
                            defaultValue = -1
                        })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getInt("userId") ?: -1

                        if (userId <= 0) {
                            navController.navigate("login") {
                                popUpTo("pasteles/{userId}") { inclusive = true }
                            }
                        } else {
                            PastelesScreen(
                                pastelesRepository = pastelesRepository,
                                carritoService = carritoService as CarritoService,
                                userId = userId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCart = {
                                    navController.navigate("carrito/$userId")
                                }
                            )
                        }
                    }

                    composable(
                        route = "carrito/{userId}",
                        arguments = listOf(navArgument("userId") {
                            type = NavType.IntType
                            defaultValue = -1
                        })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getInt("userId") ?: -1

                        if (userId <= 0) {
                            navController.navigate("login") {
                                popUpTo("carrito/{userId}") { inclusive = true }
                            }
                        } else {
                            CarritoScreen(
                                userId = userId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}