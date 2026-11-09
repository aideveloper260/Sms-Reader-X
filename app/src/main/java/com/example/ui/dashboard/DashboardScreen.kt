package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalPaymentEvent
import com.example.data.local.ProjectConfig
import com.example.data.model.StatsResponse
import com.example.data.repository.PaymentGatewayRepository
import com.example.service.SmsMonitorService
import com.example.ui.BrandFooter
import com.example.ui.theme.BkashColor
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandTextSecondary
import com.example.ui.theme.BrandTextTertiary
import com.example.ui.theme.BrandWarning
import com.example.ui.theme.NagadColor
import com.example.ui.theme.RocketColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DashboardScreen(
    repository: PaymentGatewayRepository,
    onNavigateToSms: () -> Unit,
    onAddProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by repository.configFlow.collectAsState(initial = ProjectConfig())
    val localEvents by repository.localEventsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<StatsResponse?>(null) }
    var isLoadingStats by remember { mutableStateOf(false) }
    var statsError by remember { mutableStateOf<String?>(null) }

    fun refreshStats() {
        if (!config.isConfigured) return
        isLoadingStats = true
        statsError = null
        coroutineScope.launch {
            val result = repository.fetchDashboardStats()
            isLoadingStats = false
            result.onSuccess {
                stats = it
            }.onFailure {
                statsError = it.localizedMessage ?: "Failed to refresh stats"
            }
        }
    }

    LaunchedEffect(config.isConfigured) {
        if (config.isConfigured) {
            refreshStats()
        }
    }

    val dhakaGreeting = getDhakaGreeting()
    val dhakaDateTime = getDhakaDateTimeFormatted()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 18.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Header: Time-aware greeting & date in Asia/Dhaka
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dhakaGreeting,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )
                    Text(
                        text = dhakaDateTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BrandTextSecondary
                        )
                    )
                }

                IconButton(
                    onClick = { refreshStats() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    if (isLoadingStats) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandPurplePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = BrandPurplePrimary
                        )
                    }
                }
            }
        }

        // Project Status & Monitoring Switch Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (config.isConfigured && config.isMonitoringActive) BrandSuccess else BrandError
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (config.isConfigured) config.projectName else "প্রজেক্ট কনফিগার করা হয়নি",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextPrimary
                                    )
                                )
                            }
                            Text(
                                text = if (config.isConfigured) "সংযুক্ত: ${config.apiUrl.take(30)}..." else "পেমেন্ট SMS মনিটরিং শুরু করতে প্রজেক্ট যোগ করুন",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (config.isConfigured) {
                            Switch(
                                checked = config.isMonitoringActive,
                                onCheckedChange = { active ->
                                    coroutineScope.launch {
                                        repository.setMonitoringActive(active)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandPurplePrimary
                                ),
                                modifier = Modifier.testTag("monitoring_toggle")
                            )
                        }
                    }

                    if (!config.isConfigured) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddProjectClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ নতুন প্রজেক্ট যোগ করুন", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SMS মনিটরিং: ${if (config.isMonitoringActive) "চালু" else "বন্ধ"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (config.isMonitoringActive) BrandSuccess else BrandWarning
                                )
                            )
                            Text(
                                text = "শেষ সিঙ্ক: ${config.lastSyncTime.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                            )
                        }
                    }
                }
            }
        }

        // Today's Aggregate Overview Cards
        item {
            Text(
                text = "আজকের সারসংক্ষেপ (Asia/Dhaka)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            val todayApprovedCount = stats?.byStatus?.get("APPROVED") ?: 0
            val todayPendingCount = (stats?.byStatus?.get("PENDING_REVIEW") ?: 0) + (stats?.byStatus?.get("UNMATCHED") ?: 0)
            val todayTotalReceivedAmount = stats?.todayAmount ?: "0.00"
            val totalEligibleCount = stats?.total ?: localEvents.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "মোট প্রাপ্ত SMS",
                    value = "$totalEligibleCount টি",
                    color = BrandPurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "অনুমোদিত পেমেন্ট",
                    value = "$todayApprovedCount টি",
                    color = BrandSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "অপেক্ষমাণ / রিভিউ",
                    value = "$todayPendingCount টি",
                    color = BrandWarning,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "মোট পেমেন্ট (৳)",
                    value = "৳$todayTotalReceivedAmount",
                    color = BrandPurplePrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Provider Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "প্রোভাইডার পরিসংখ্যান",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val bkashStat = stats?.byProvider?.get("bkash")
                    val nagadStat = stats?.byProvider?.get("nagad")
                    val rocketStat = stats?.byProvider?.get("rocket")

                    ProviderStatRow(
                        name = "bKash",
                        color = BkashColor,
                        count = bkashStat?.count ?: 0,
                        amount = bkashStat?.amount ?: "0.00"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProviderStatRow(
                        name = "Nagad",
                        color = NagadColor,
                        count = nagadStat?.count ?: 0,
                        amount = nagadStat?.amount ?: "0.00"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProviderStatRow(
                        name = "Rocket",
                        color = RocketColor,
                        count = rocketStat?.count ?: 0,
                        amount = rocketStat?.amount ?: "0.00"
                    )
                }
            }
        }

        // Recent Activity List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক পেমেন্ট ইভেন্ট",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Text(
                    text = "সবগুলো দেখুন (${localEvents.size})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrandPurplePrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToSms() }
                )
            }
        }

        if (localEvents.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = BrandTextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এখনো কোনো পেমেন্ট SMS রিসিভ হয়নি",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BrandTextSecondary)
                        )
                        Text(
                            text = "bKash, Nagad বা Rocket-এ টাকা আসলে এখানে স্বয়ংক্রিয়ভাবে ভেসে উঠবে",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                        )
                    }
                }
            }
        } else {
            items(localEvents.take(5)) { event ->
                RecentEventCard(event = event)
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            BrandFooter()
            Spacer(modifier = Modifier.height(60.dp)) // padding for bottom bar
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(color = BrandTextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun ProviderStatRow(
    name: String,
    color: Color,
    count: Int,
    amount: String
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
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                )
            )
        }

        Row {
            Text(
                text = "$count টি",
                style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "৳$amount",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
            )
        }
    }
}

@Composable
private fun RecentEventCard(event: LocalPaymentEvent) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val providerColor = when (event.provider.lowercase(Locale.ROOT)) {
                    "bkash" -> BkashColor
                    "nagad" -> NagadColor
                    "rocket" -> RocketColor
                    else -> BrandPurplePrimary
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(providerColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = event.provider.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = providerColor
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "৳${event.amount} (${event.provider.replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )
                    Text(
                        text = "TrxID: ${event.transactionId.take(4)}****${event.transactionId.takeLast(4)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandTextSecondary
                        )
                    )
                }
            }

            // Status Badge
            val (badgeText, badgeBg, badgeTextColor) = when (event.syncStatus) {
                "SENT_APPROVED" -> Triple("Approved", Color(0xFFD1FAE5), BrandSuccess)
                "SENT_UNMATCHED" -> Triple("Unmatched", Color(0xFFFEF3C7), BrandWarning)
                "SENT_REVIEW" -> Triple("Review", Color(0xFFEDE9FE), BrandPurplePrimary)
                "SENT_DUPLICATE" -> Triple("Duplicate", Color(0xFFFEE2E2), BrandError)
                "SENDING" -> Triple("Sending", Color(0xFFE0E7FF), BrandPurpleLight)
                else -> Triple("Queued", Color(0xFFF3F4F6), BrandTextSecondary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                )
            }
        }
    }
}

private fun getDhakaGreeting(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "শুভ সকাল"
        in 12..14 -> "শুভ দুপুর"
        in 15..17 -> "শুভ বিকাল"
        in 18..20 -> "শুভ সন্ধ্যা"
        else -> "শুভ রাত্রি"
    }
}

private fun getDhakaDateTimeFormatted(): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy • hh:mm a", Locale("bn", "BD"))
    sdf.timeZone = TimeZone.getTimeZone("Asia/Dhaka")
    return sdf.format(Date())
}
