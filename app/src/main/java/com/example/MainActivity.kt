package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PantryHomeScreen
import com.example.ui.PantryStatsScreen
import com.example.ui.PantryViewModel
import com.example.ui.RescueRecipesScreen
import com.example.ui.ScannerScreen
import com.example.ui.SplashScreen
import com.example.ui.engagement.EngagementManager
import com.example.ui.engagement.RateAppDialog
import com.example.ui.engagement.ShareAppDialog
import com.example.ui.engagement.SuggestionType
import com.example.ui.engagement.UpdateAppDialog
import kotlinx.coroutines.delay
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ShelfLifeTheme

enum class ShelfTab(val titleResId: Int, val icon: ImageVector, val tag: String) {
    PANTRY(R.string.tab_pantry, Icons.Default.Kitchen, "nav_pantry"),
    SCANNER(R.string.tab_scan, Icons.Default.QrCodeScanner, "nav_scan"),
    RECIPES(R.string.tab_rescue, Icons.Default.Restaurant, "nav_recipes"),
    STATS(R.string.tab_impact, Icons.Default.Eco, "nav_stats")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShelfLifeTheme {
                var isSplashActive by remember { mutableStateOf(true) }

                AnimatedContent(
                    targetState = isSplashActive,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "splashTransition"
                ) { showSplash ->
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { isSplashActive = false })
                    } else {
                        ShelfLifeApp()
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfLifeApp(viewModel: PantryViewModel = viewModel()) {
    val context = LocalContext.current
    val engagementManager = remember { EngagementManager(context) }

    var currentTab by remember { mutableStateOf(ShelfTab.PANTRY) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()
    val expiringItems by viewModel.expiringSoonItems.collectAsState()

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    var showAutoRateDialog by remember { mutableStateOf(false) }
    var showAutoShareDialog by remember { mutableStateOf(false) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var autoUpdateTargetVersion by remember { mutableStateOf("") }

    // Evaluates next suggestion according to usage milestones, priority and cooldowns
    fun checkAndTriggerAutoSuggestion() {
        when (val suggestion = engagementManager.checkNextAutoSuggestion(currentVersionName)) {
            SuggestionType.UPDATE -> {
                val target = engagementManager.getLatestAvailableVersion() ?: "1.1"
                autoUpdateTargetVersion = target
                engagementManager.markSuggestionShown(SuggestionType.UPDATE, target)
                showAutoUpdateDialog = true
            }
            SuggestionType.RATE -> {
                engagementManager.markSuggestionShown(SuggestionType.RATE)
                showAutoRateDialog = true
            }
            SuggestionType.SHARE -> {
                engagementManager.markSuggestionShown(SuggestionType.SHARE)
                showAutoShareDialog = true
            }
            SuggestionType.NONE -> Unit
        }
    }

    // Record launch and evaluate smart prompt triggers after a non-intrusive settle delay
    LaunchedEffect(Unit) {
        engagementManager.recordLaunch()
        delay(2500) // Never interrupt user during initial cold start transition
        checkAndTriggerAutoSuggestion()
    }

    // Handle back button: if not on Pantry tab, return to Pantry tab
    BackHandler(enabled = currentTab != ShelfTab.PANTRY) {
        currentTab = ShelfTab.PANTRY
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            if (msg.contains("Saved", ignoreCase = true) || msg.contains("waste", ignoreCase = true)) {
                engagementManager.recordRescuedItem()
                checkAndTriggerAutoSuggestion()
            } else if (msg.contains("Added", ignoreCase = true)) {
                engagementManager.recordItemAdded()
            }
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ShelfTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            val isRescueTab = tab == ShelfTab.RECIPES
                            val badgeCount = expiringItems.size
                            val title = stringResource(tab.titleResId)

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (isRescueTab && badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = Color(0xFFD97706),
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$badgeCount")
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = tab.icon, contentDescription = title)
                                        }
                                    } else {
                                        Icon(imageVector = tab.icon, contentDescription = title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NavigationRail for tablets, foldables, and desktop/DeX/TV
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("tablet_navigation_rail")
                    ) {
                        ShelfTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            val isRescueTab = tab == ShelfTab.RECIPES
                            val badgeCount = expiringItems.size
                            val title = stringResource(tab.titleResId)

                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (isRescueTab && badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = Color(0xFFD97706),
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$badgeCount")
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = tab.icon, contentDescription = title)
                                        }
                                    } else {
                                        Icon(imageVector = tab.icon, contentDescription = title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                // Main Content area centered with max width bounds for adaptive readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 1200.dp)
                            .fillMaxSize()
                    ) {
                        Crossfade(
                            targetState = currentTab,
                            label = "tabCrossfade"
                        ) { tab ->
                            when (tab) {
                                ShelfTab.PANTRY -> PantryHomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToScanner = { currentTab = ShelfTab.SCANNER },
                                    onNavigateToRecipes = { currentTab = ShelfTab.RECIPES }
                                )
                                ShelfTab.SCANNER -> ScannerScreen(
                                    viewModel = viewModel,
                                    onItemAdded = { currentTab = ShelfTab.PANTRY }
                                )
                                ShelfTab.RECIPES -> RescueRecipesScreen(
                                    viewModel = viewModel
                                )
                                ShelfTab.STATS -> PantryStatsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAutoUpdateDialog) {
        val targetVer = autoUpdateTargetVersion.ifBlank { "1.1" }
        UpdateAppDialog(
            latestVersion = targetVer,
            onDismiss = {
                engagementManager.onUserDismissedUpdate(targetVer)
                showAutoUpdateDialog = false
            },
            onUpdate = {
                engagementManager.onUserUpdated(targetVer)
                showAutoUpdateDialog = false
            }
        )
    }

    if (showAutoRateDialog) {
        RateAppDialog(
            onDismiss = {
                engagementManager.onUserDismissedRating(false)
                showAutoRateDialog = false
            },
            onRateNow = {
                engagementManager.onUserRated()
                showAutoRateDialog = false
            },
            onNeverAskAgain = {
                engagementManager.onUserDismissedRating(true)
                showAutoRateDialog = false
            }
        )
    }

    if (showAutoShareDialog) {
        ShareAppDialog(
            onDismiss = {
                engagementManager.onUserDismissedShare()
                showAutoShareDialog = false
            },
            onShare = {
                engagementManager.onUserShared()
                showAutoShareDialog = false
            }
        )
    }
}
