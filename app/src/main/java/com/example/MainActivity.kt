package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionMode
import com.example.ui.RouterViewModel
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.ModificationCenterScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnisocTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RouterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnisocTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

sealed class NavTab(val index: Int, val title: String, val icon: ImageVector, val tag: String) {
    object Dashboard : NavTab(0, "Dashboard", Icons.Default.Dashboard, "tab_dashboard")
    object Tweaks : NavTab(1, "Tweaks", Icons.Default.Tune, "tab_tweaks")
    object Discovery : NavTab(2, "AI Scan", Icons.Default.AutoAwesome, "tab_discovery")
    object Terminal : NavTab(3, "Terminal", Icons.Default.Terminal, "tab_terminal")
    object Diagnostics : NavTab(4, "Latency", Icons.Default.Analytics, "tab_diagnostics")
    object Advisor : NavTab(5, "AI Brain", Icons.Default.Psychology, "tab_advisor")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: RouterViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    var terminalPreloadCommand by remember { mutableStateOf<String?>(null) }

    // BackHandler: If on a sub-tab, pressing system back returns to Dashboard (tab 0)
    BackHandler(enabled = selectedTab != 0) {
        viewModel.setSelectedTab(0)
    }

    val tabs = listOf(
        NavTab.Dashboard,
        NavTab.Tweaks,
        NavTab.Discovery,
        NavTab.Terminal,
        NavTab.Diagnostics,
        NavTab.Advisor
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UNISOC AI Master",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "UDX710 5G/LTE · Telnet Center",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    val statusDotColor = when (systemInfo.connectionMode) {
                        ConnectionMode.CONNECTED_REAL -> StatusSuccess
                        ConnectionMode.CONNECTED_SIMULATED -> StatusWarning
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusDotColor.copy(alpha = 0.15f))
                            .border(1.dp, statusDotColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (systemInfo.connectionMode == ConnectionMode.CONNECTED_REAL) "192.168.0.1:23" else "Emulation",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = statusDotColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.safeDrawing
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(tab.index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            selectedIconColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = NeonCyan,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBg)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContentTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateTab = { viewModel.setSelectedTab(it) }
                    )
                    1 -> ModificationCenterScreen(
                        viewModel = viewModel
                    )
                    2 -> DiscoveryScreen(
                        viewModel = viewModel,
                        onOpenTerminalCommand = { cmd ->
                            terminalPreloadCommand = cmd
                            viewModel.executeTerminalCommand(cmd)
                            viewModel.setSelectedTab(3) // Switch to terminal
                        }
                    )
                    3 -> TerminalScreen(
                        viewModel = viewModel,
                        initialCommand = terminalPreloadCommand
                    )
                    4 -> DiagnosticsScreen(
                        viewModel = viewModel
                    )
                    5 -> AiAdvisorScreen(
                        viewModel = viewModel,
                        onExecuteInTerminal = { cmd ->
                            terminalPreloadCommand = cmd
                            viewModel.executeTerminalCommand(cmd)
                            viewModel.setSelectedTab(3) // Switch to terminal
                        }
                    )
                }
            }
        }
    }
}
