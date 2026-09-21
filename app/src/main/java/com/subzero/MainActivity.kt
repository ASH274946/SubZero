package com.subzero

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.subzero.data.MandateEntity
import com.subzero.receivers.MandateReminderReceiver
import com.subzero.ui.screens.*
import com.subzero.ui.theme.M3CanvasBackground
import com.subzero.ui.theme.SubZeroTheme
import kotlinx.coroutines.launch

/**
 * Main Activity container for SubZero: Autonomous On-Device Mandate Guardian.
 */
class MainActivity : ComponentActivity() {

    sealed class Screen {
        data object Setup : Screen()
        data object Home : Screen()
        data object DemoPaywall : Screen()
        data object Settings : Screen()
        data object KillSwitchQueue : Screen()
        data class CancelGuideScreen(val mandate: MandateEntity) : Screen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isFirstLaunch = getSharedPreferences("subzero_prefs", Context.MODE_PRIVATE)
            .getBoolean("setup_completed", false).not()

        val routeExtra = intent?.getStringExtra(MandateReminderReceiver.EXTRA_ROUTE)
        val defaultScreen = when {
            routeExtra == MandateReminderReceiver.ROUTE_KILL_SWITCH_QUEUE -> Screen.KillSwitchQueue
            isFirstLaunch -> Screen.Setup
            else -> Screen.Home
        }

        setContent {
            SubZeroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = M3CanvasBackground
                ) {
                    SubZeroAppNavigation(
                        initialScreen = defaultScreen,
                        onSetupCompletePreference = {
                            getSharedPreferences("subzero_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("setup_completed", true)
                                .apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubZeroAppNavigation(
    initialScreen: MainActivity.Screen = MainActivity.Screen.Home,
    onSetupCompletePreference: () -> Unit = {}
) {
    val backStack = remember {
        mutableStateListOf<MainActivity.Screen>().apply {
            if (initialScreen is MainActivity.Screen.KillSwitchQueue) {
                add(MainActivity.Screen.Home)
                add(initialScreen)
            } else {
                add(initialScreen)
            }
        }
    }

    val currentScreen = backStack.lastOrNull() ?: MainActivity.Screen.Home

    // Synchronize Mobile's Native Gesture Back Navigation with Application Backstack
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    fun navigateTo(screen: MainActivity.Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is MainActivity.Screen.Setup -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                SetupScreen(
                    onSetupCompleted = {
                        onSetupCompletePreference()
                        scope.launch {
                            com.subzero.engine.WalkthroughEngine.seedInitialMandatesIfEmpty(context)
                        }
                        backStack.clear()
                        backStack.add(MainActivity.Screen.Home)
                    }
                )
            }
            is MainActivity.Screen.Home -> {
                HomeScreen(
                    onLaunchWalkthrough = { navigateTo(MainActivity.Screen.DemoPaywall) },
                    onNavigateToDemoPaywall = { navigateTo(MainActivity.Screen.DemoPaywall) },
                    onNavigateToMockPaywall = { navigateTo(MainActivity.Screen.DemoPaywall) },
                    onNavigateToSettings = { navigateTo(MainActivity.Screen.Settings) },
                    onNavigateToAssistedRevoke = { mandate ->
                        navigateTo(MainActivity.Screen.CancelGuideScreen(mandate))
                    },
                    onNavigateToKillSwitchQueue = {
                        navigateTo(MainActivity.Screen.KillSwitchQueue)
                    }
                )
            }
            is MainActivity.Screen.KillSwitchQueue -> {
                KillSwitchQueueScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is MainActivity.Screen.DemoPaywall -> {
                DemoPaywallScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is MainActivity.Screen.Settings -> {
                PermissionsScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is MainActivity.Screen.CancelGuideScreen -> {
                CancelGuide(
                    mandate = screen.mandate,
                    onClose = { navigateBack() }
                )
            }
        }
    }
}
