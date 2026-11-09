package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ProjectConfig
import com.example.data.repository.PaymentGatewayRepository
import com.example.service.SmsMonitorService
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.project.AddProjectScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.sms.SmsScreen
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandTextSecondary
import com.example.ui.theme.SMSReaderXTheme
import com.example.ui.welcome.WelcomeScreen

enum class AppNavScreen {
    WELCOME,
    ADD_PROJECT,
    DASHBOARD,
    SMS_LIST,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = PaymentGatewayRepository(applicationContext)

        setContent {
            SMSReaderXTheme {
                MainAppNavHost(repository = repository)
            }
        }
    }
}

@Composable
fun MainAppNavHost(repository: PaymentGatewayRepository) {
    val context = LocalContext.current
    val config by repository.configFlow.collectAsState(initial = ProjectConfig())

    var currentScreen by remember { mutableStateOf(AppNavScreen.DASHBOARD) }

    // Multi-permission launcher for SMS & Notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsReceiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val smsReadGranted = permissions[Manifest.permission.READ_SMS] ?: false
        if (smsReceiveGranted && config.isConfigured && config.isMonitoringActive) {
            SmsMonitorService.start(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Determine initial destination
    LaunchedEffect(config.isConfigured) {
        if (!config.isConfigured && currentScreen != AppNavScreen.ADD_PROJECT) {
            currentScreen = AppNavScreen.WELCOME
        } else if (config.isConfigured && currentScreen == AppNavScreen.WELCOME) {
            currentScreen = AppNavScreen.DASHBOARD
            if (config.isMonitoringActive) {
                SmsMonitorService.start(context)
            }
        }
    }

    // Handle system back button gracefully
    BackHandler(enabled = currentScreen != AppNavScreen.DASHBOARD && currentScreen != AppNavScreen.WELCOME) {
        currentScreen = if (config.isConfigured) AppNavScreen.DASHBOARD else AppNavScreen.WELCOME
    }

    Scaffold(
        bottomBar = {
            if (config.isConfigured && currentScreen in listOf(AppNavScreen.DASHBOARD, AppNavScreen.SMS_LIST, AppNavScreen.SETTINGS)) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.DASHBOARD,
                        onClick = { currentScreen = AppNavScreen.DASHBOARD },
                        icon = {
                            Icon(
                                if (currentScreen == AppNavScreen.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("ড্যাশবোর্ড", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurplePrimary,
                            selectedTextColor = BrandPurplePrimary,
                            indicatorColor = BrandPurpleContainer,
                            unselectedIconColor = BrandTextSecondary,
                            unselectedTextColor = BrandTextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.SMS_LIST,
                        onClick = { currentScreen = AppNavScreen.SMS_LIST },
                        icon = {
                            Icon(
                                if (currentScreen == AppNavScreen.SMS_LIST) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "SMS List"
                            )
                        },
                        label = { Text("SMS তালিকা", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurplePrimary,
                            selectedTextColor = BrandPurplePrimary,
                            indicatorColor = BrandPurpleContainer,
                            unselectedIconColor = BrandTextSecondary,
                            unselectedTextColor = BrandTextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.SETTINGS,
                        onClick = { currentScreen = AppNavScreen.SETTINGS },
                        icon = {
                            Icon(
                                if (currentScreen == AppNavScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("সেটিংস", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurplePrimary,
                            selectedTextColor = BrandPurplePrimary,
                            indicatorColor = BrandPurpleContainer,
                            unselectedIconColor = BrandTextSecondary,
                            unselectedTextColor = BrandTextSecondary
                        )
                    )
                }
            }
        },
        containerColor = BrandBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppNavScreen.WELCOME -> {
                    WelcomeScreen(
                        onAddProjectClick = { currentScreen = AppNavScreen.ADD_PROJECT }
                    )
                }
                AppNavScreen.ADD_PROJECT -> {
                    AddProjectScreen(
                        repository = repository,
                        onProjectSaved = {
                            currentScreen = AppNavScreen.DASHBOARD
                            SmsMonitorService.start(context)
                        },
                        onCancel = {
                            currentScreen = if (config.isConfigured) AppNavScreen.DASHBOARD else AppNavScreen.WELCOME
                        }
                    )
                }
                AppNavScreen.DASHBOARD -> {
                    DashboardScreen(
                        repository = repository,
                        onNavigateToSms = { currentScreen = AppNavScreen.SMS_LIST },
                        onAddProjectClick = { currentScreen = AppNavScreen.ADD_PROJECT }
                    )
                }
                AppNavScreen.SMS_LIST -> {
                    SmsScreen(repository = repository)
                }
                AppNavScreen.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        onAddNewProjectClick = { currentScreen = AppNavScreen.ADD_PROJECT }
                    )
                }
            }
        }
    }
}
