package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CarriersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.InternetVaultTheme
import com.example.ui.theme.VaultElectricBlue
import kotlinx.coroutines.launch

private enum class VaultTab(
    val route: String,
    val labelPt: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = "dashboard",
        labelPt = "Painel",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    CARRIERS(
        route = "carriers",
        labelPt = "Operadoras",
        selectedIcon = Icons.Filled.CellTower,
        unselectedIcon = Icons.Outlined.CellTower,
        testTag = "nav_carriers"
    ),
    SERVERS(
        route = "servers",
        labelPt = "Servidores",
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Dns,
        testTag = "nav_servers"
    ),
    HISTORY(
        route = "history",
        labelPt = "Histórico",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history"
    ),
    SETTINGS(
        route = "settings",
        labelPt = "Definições",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
}

@Composable
fun InternetVaultRoot(viewModel: VaultViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.onConnectButtonPressed()
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val json = viewModel.exportVaultJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    snackbarHostState.showSnackbar("Configurações exportadas com sucesso.")
                }.onFailure {
                    snackbarHostState.showSnackbar("Erro ao exportar ficheiro JSON.")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.use { inp ->
                        inp.bufferedReader().readText()
                    }.orEmpty()
                    viewModel.importVaultJson(json)
                }.onFailure {
                    snackbarHostState.showSnackbar("Não foi possível ler o ficheiro seleccionado.")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VaultUiEvent.RequestVpnPermission -> {
                    vpnPermissionLauncher.launch(event.intent)
                }
                is VaultUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val handleConnectClick: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onConnectButtonPressed()
            }
        } else {
            viewModel.onConnectButtonPressed()
        }
    }

    InternetVaultTheme(themeMode = uiState.preferences.themeMode) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val navigateToTab: (VaultTab) -> Unit = { tab ->
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            VaultTab.entries.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == tab.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelPt
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelPt,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VaultElectricBlue,
                                        selectedTextColor = VaultElectricBlue,
                                        indicatorColor = VaultElectricBlue.copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
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
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            VaultTab.entries.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == tab.route
                                } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelPt
                                        )
                                    },
                                    label = { Text(tab.labelPt) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = VaultElectricBlue,
                                        selectedTextColor = VaultElectricBlue,
                                        indicatorColor = VaultElectricBlue.copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = VaultTab.DASHBOARD.route,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(VaultTab.DASHBOARD.route) {
                            DashboardScreen(
                                uiState = uiState,
                                onToggleVpn = handleConnectClick,
                                onRefreshNetwork = { viewModel.probeSelectedServerAndInternet() },
                                onNavigateToCarriers = { navigateToTab(VaultTab.CARRIERS) },
                                onNavigateToServers = { navigateToTab(VaultTab.SERVERS) },
                                onNavigateToHistory = { navigateToTab(VaultTab.HISTORY) }
                            )
                        }
                        composable(VaultTab.CARRIERS.route) {
                            CarriersScreen(
                                uiState = uiState,
                                sponsoredPartnerships = viewModel.sponsoredPartnerships,
                                onSelectCarrierMode = viewModel::selectCarrierMode,
                                onPrepareVaultOverWifi = viewModel::prepareVaultOverWifi
                            )
                        }
                        composable(VaultTab.SERVERS.route) {
                            ServersScreen(
                                uiState = uiState,
                                onSelectServer = viewModel::selectServer,
                                onSaveServer = viewModel::saveServer,
                                onDeleteServer = viewModel::deleteServer,
                                onTestServer = viewModel::testSingleServer,
                                onTestAllServers = viewModel::testAllServers,
                                onExportJsonClick = {
                                    exportJsonLauncher.launch("internet_vault_config.json")
                                },
                                onImportJsonClick = {
                                    importJsonLauncher.launch(arrayOf("application/json", "text/plain"))
                                }
                            )
                        }
                        composable(VaultTab.HISTORY.route) {
                            HistoryScreen(
                                uiState = uiState,
                                onClearHistory = viewModel::clearUsageHistory
                            )
                        }
                        composable(VaultTab.SETTINGS.route) {
                            SettingsScreen(
                                uiState = uiState,
                                onRunDiagnostics = viewModel::runFullDiagnostics,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetAutoPrepareWifi = viewModel::setAutoPrepareOnWifi,
                                onSetDnsLeakGuard = viewModel::setDnsLeakGuard,
                                onSetBypassLan = viewModel::setBypassLan
                            )
                        }
                    }
                }
            }
        }
    }
}
