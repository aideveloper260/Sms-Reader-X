package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectConfig
import com.example.data.repository.PaymentGatewayRepository
import com.example.ui.BrandFooter
import com.example.ui.theme.BkashColor
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandTextSecondary
import com.example.ui.theme.BrandTextTertiary
import com.example.ui.theme.BrandWarning
import com.example.ui.theme.NagadColor
import com.example.ui.theme.RocketColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: PaymentGatewayRepository,
    onAddNewProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by repository.configFlow.collectAsState(initial = ProjectConfig())
    val coroutineScope = rememberCoroutineScope()

    var showDisconnectDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }
    var isConnectionOk by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "সেটিংস ও কনফিগারেশন",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )
        )
        Text(
            text = "প্রজেক্ট, পারমিশন ও ব্যাকএন্ড সংযোগ ব্যবস্থাপনা",
            style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Current Project Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (config.isConfigured) config.projectName else "কোনো সক্রিয় প্রজেক্ট নেই",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                        )
                        if (config.isConfigured) {
                            Text(
                                text = "API Host: ${config.apiUrl}",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Merchant: ${config.merchantId} • Device ID: ${config.deviceId.take(12)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (config.isConfigured) BrandSuccess else BrandWarning)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddNewProjectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (config.isConfigured) "প্রজেক্ট পরিবর্তন" else "প্রজেক্ট যোগ", fontSize = 13.sp)
                    }

                    if (config.isConfigured) {
                        OutlinedButton(
                            onClick = { showDisconnectDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandError),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "মুছে ফেলুন", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Permissions Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "পারমিশন স্ট্যাটাস",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                PermissionRowItem(
                    icon = Icons.Default.Sms,
                    title = "SMS পারমিশন",
                    subtitle = "bKash, Nagad ও Rocket SMS পড়া এবং যাচাই করার জন্য"
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRowItem(
                    icon = Icons.Default.Notifications,
                    title = "নোটিফিকেশন পারমিশন",
                    subtitle = "ব্যাকগ্রাউন্ড মনিটরিং স্ট্যাটাস চালু রাখার জন্য"
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "অ্যান্ড্রয়েড অ্যাপ পারমিশন ওপেন করুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SMS & Provider Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SMS পর্যবেক্ষণ ও প্রোভাইডার",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Active Providers
                ProviderToggleRow(
                    name = "bKash পেমেন্ট রিড",
                    color = BkashColor,
                    enabled = config.enableBkash,
                    onToggle = { coroutineScope.launch { repository.setProviders(it, config.enableNagad, config.enableRocket) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProviderToggleRow(
                    name = "Nagad পেমেন্ট রিড",
                    color = NagadColor,
                    enabled = config.enableNagad,
                    onToggle = { coroutineScope.launch { repository.setProviders(config.enableBkash, it, config.enableRocket) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProviderToggleRow(
                    name = "Rocket পেমেন্ট রিড",
                    color = RocketColor,
                    enabled = config.enableRocket,
                    onToggle = { coroutineScope.launch { repository.setProviders(config.enableBkash, config.enableNagad, it) } }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Start on boot toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ডিভাইস রিবুটের পর চালু করুন",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTextPrimary
                            )
                        )
                        Text(
                            text = "ফোন রিস্টার্ট হলে স্বয়ংক্রিয়ভাবে ব্যাকগ্রাউন্ড সার্ভিস শুরু হবে",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrandTextSecondary)
                        )
                    }

                    Switch(
                        checked = config.startOnBoot,
                        onCheckedChange = { coroutineScope.launch { repository.setStartOnBoot(it) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = BrandPurplePrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { coroutineScope.launch { repository.clearDeliveredCache() } },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "লোকাল ডেলিভার্ড ক্যাশ মুছে ফেলুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Backend Health & Connection Test
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ব্যাকএন্ড সংযোগ টেস্ট",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Text(
                    text = "পাইথন গেটওয়ের /v1/health এন্ডপয়েন্ট পিং করে পরীক্ষা করুন",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!config.isConfigured) {
                            connectionTestResult = "কোনো প্রজেক্ট কনফিগার করা নেই।"
                            isConnectionOk = false
                            return@Button
                        }
                        isTestingConnection = true
                        connectionTestResult = null
                        coroutineScope.launch {
                            val res = repository.fetchDashboardStats()
                            isTestingConnection = false
                            res.onSuccess {
                                isConnectionOk = true
                                connectionTestResult = "সংযোগ সফল! সার্ভার রেসপন্স করছে (Asia/Dhaka)।"
                            }.onFailure {
                                isConnectionOk = false
                                connectionTestResult = "সংযোগ ব্যর্থ: ${it.localizedMessage}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                    enabled = !isTestingConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সার্ভার সংযোগ টেস্ট করুন")
                    }
                }

                if (connectionTestResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionTestResult!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isConnectionOk) BrandSuccess else BrandError,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. About Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SMS READER X • সংস্করণ ১.০",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "এই অ্যাপটি কেবল আপনার নিজের অনুমোদিত রিসিভিং সিমের bKash, Nagad এবং Rocket পেমেন্ট নোটিফিকেশন এসএমএস রিড করে এবং আপনার নিজস্ব Python Gateway-তে পাঠায়। কোনো ওটিপি, ব্যক্তিগত বার্তা বা সংবেদনশীল মেসেজ ক্লাউডে পাঠানো হয় না।",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrandTextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        BrandFooter()
        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("প্রজেক্ট মুছে ফেলতে চান?") },
            text = { Text("এটি বর্তমান API URL ও ডিভাইস ক্রেডেনশিয়াল মুছে ফেলবে এবং SMS মনিটরিং বন্ধ করবে। ব্যাকএন্ডের আসল ট্রানজেকশন ডেটা নষ্ট হবে না।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.removeProject()
                            showDisconnectDialog = false
                        }
                    }
                ) {
                    Text("হ্যাঁ, মুছুন", color = BrandError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun PermissionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = BrandPurplePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = BrandTextPrimary))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = BrandTextSecondary, fontSize = 11.sp))
        }
    }
}

@Composable
private fun ProviderToggleRow(
    name: String,
    color: Color,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = BrandTextPrimary
                )
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = BrandPurplePrimary)
        )
    }
}
