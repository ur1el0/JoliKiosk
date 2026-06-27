package com.example.webdev2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

val Pink = Color(0xFF750000)
val PinkLight = Color(0xFFB60000)
val BgGray = Color(0xFFF6F6F6)
val CardWhite = Color.White
val TextDark = Color(0xFF1A1A1A)
val TextGray = Color(0xFF666666)
val ChipSel = Color(0xFFFFE4E4)

private sealed class Screen(val route: String) {
    data object Menu : Screen("menu")
    data object Cart : Screen("cart")
}

@Composable
fun AppNavigation() {
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }

    AnimatedContent(
        targetState = selectedBranch,
        transitionSpec = {
            fadeIn(tween(400)) + scaleIn(initialScale = 0.96f) togetherWith fadeOut(tween(250))
        },
        label = "branch-transition",
    ) { branch ->
        if (branch == null) {
            BranchSelectionScreen(onBranchSelected = { selectedBranch = it })
        } else {
            KioskContent(branch = branch, onChangeBranch = { selectedBranch = null })
        }
    }
}

@Composable
private fun KioskContent(branch: Branch, onChangeBranch: () -> Unit) {
    val api = remember { KioskApi() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val quantities = remember { mutableStateMapOf<Int, Int>() }
    var menu by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var submittedOrder by remember { mutableStateOf<SubmittedOrder?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    suspend fun loadMenu() {
        isLoading = true
        error = null
        runCatching { api.menu() }
            .onSuccess { menu = it }
            .onFailure { error = it.message ?: "Unable to load the menu." }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadMenu() }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        containerColor = BgGray,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = CardWhite, tonalElevation = 0.dp) {
                listOf(Screen.Menu to "Menu", Screen.Cart to "Cart").forEach { (screen, label) ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Menu.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        icon = {
                            if (screen == Screen.Cart) {
                                BadgedBox(badge = {
                                    val count = quantities.values.sum()
                                    if (count > 0) {
                                        Badge(containerColor = Pink) {
                                            Text(count.toString(), color = Color.White)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                                }
                            } else {
                                Icon(Icons.Outlined.Home, contentDescription = "Menu")
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Menu.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Menu.route) {
                MenuScreen(
                    menu = menu,
                    quantities = quantities,
                    branch = branch,
                    isLoading = isLoading,
                    error = error,
                    onRetry = { scope.launch { loadMenu() } },
                    onChangeBranch = onChangeBranch,
                    onAdd = { item ->
                        quantities[item.id] = (quantities[item.id] ?: 0) + 1
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = "${item.name} added to cart",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    menu = menu,
                    quantities = quantities,
                    isSubmitting = isSubmitting,
                    error = error,
                    onQuantityChange = { id, quantity ->
                        if (quantity <= 0) quantities.remove(id) else quantities[id] = quantity
                    },
                    onCheckout = {
                        isSubmitting = true
                        error = null
                        scope.launch {
                            runCatching { api.submitOrder(quantities) }
                                .onSuccess {
                                    submittedOrder = it
                                    quantities.clear()
                                }
                                .onFailure {
                                    error = it.message ?: "Unable to submit your order."
                                }
                            isSubmitting = false
                        }
                    },
                )
            }
        }
    }

    submittedOrder?.let { order ->
        OrderConfirmation(order) {
            submittedOrder = null
            navController.navigate(Screen.Menu.route) {
                popUpTo(Screen.Menu.route) { inclusive = true }
            }
        }
    }
}
