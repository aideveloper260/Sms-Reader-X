package com.example.ui.sms

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.HistoryItem
import com.example.data.repository.PaymentGatewayRepository
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
import java.util.Locale

@Composable
fun SmsScreen(
    repository: PaymentGatewayRepository,
    modifier: Modifier = Modifier
) {
    val config by repository.configFlow.collectAsState(initial = ProjectConfig())
    val localEvents by repository.localEventsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf<String?>(null) } // null = all, "bkash", "nagad", "rocket"
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }   // null = all, "APPROVED", "UNMATCHED", "PENDING_REVIEW"

    var remoteHistory by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var isLoadingRemote by remember { mutableStateOf(false) }

    fun refreshRemoteHistory() {
        if (!config.isConfigured) return
        isLoadingRemote = true
        coroutineScope.launch {
            val result = repository.fetchHistory(
                provider = selectedProviderFilter,
                status = selectedStatusFilter,
                query = searchQuery.ifBlank { null }
            )
            isLoadingRemote = false
            result.onSuccess {
                remoteHistory = it
            }
        }
    }

    LaunchedEffect(selectedProviderFilter, selectedStatusFilter, searchQuery) {
        refreshRemoteHistory()
    }

    // Filter local events
    val filteredLocalEvents = remember(localEvents, searchQuery, selectedProviderFilter, selectedStatusFilter) {
        localEvents.filter { event ->
            val matchesSearch = searchQuery.isBlank() ||
                    event.transactionId.contains(searchQuery, ignoreCase = true) ||
                    event.amount.contains(searchQuery)
            val matchesProvider = selectedProviderFilter == null || event.provider.equals(selectedProviderFilter, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || event.syncStatus.contains(selectedStatusFilter!!, ignoreCase = true)

            matchesSearch && matchesProvider && matchesStatus
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 16.dp)
            .testTag("sms_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title and Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "পেমেন্ট SMS তালিকা",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
                Text(
                    text = "মোট ফিল্টারকৃত: ${filteredLocalEvents.size} টি",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary)
                )
            }

            IconButton(
                onClick = { refreshRemoteHistory() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                if (isLoadingRemote) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BrandPurplePrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Events",
                        tint = BrandPurplePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("TrxID বা টাকার পরিমাণ দিয়ে খুঁজুন...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = BrandPurplePrimary)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sms_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips (Provider & Status)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedProviderFilter == null,
                onClick = { selectedProviderFilter = null },
                label = { Text("সব") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandPurplePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedProviderFilter == "bkash",
                onClick = { selectedProviderFilter = if (selectedProviderFilter == "bkash") null else "bkash" },
                label = { Text("bKash") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BkashColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedProviderFilter == "nagad",
                onClick = { selectedProviderFilter = if (selectedProviderFilter == "nagad") null else "nagad" },
                label = { Text("Nagad") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NagadColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedProviderFilter == "rocket",
                onClick = { selectedProviderFilter = if (selectedProviderFilter == "rocket") null else "rocket" },
                label = { Text("Rocket") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RocketColor,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Event List
        if (filteredLocalEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = BrandTextTertiary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "কোনো পেমেন্ট SMS রেকর্ড নেই",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextSecondary
                        )
                    )
                    Text(
                        text = "ফিল্টার পরিবর্তন করুন অথবা নতুন পেমেন্টের জন্য অপেক্ষা করুন",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLocalEvents, key = { it.eventId }) { event ->
                    SmsPaymentDetailCard(event = event)
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    BrandFooter()
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SmsPaymentDetailCard(event: LocalPaymentEvent) {
    val providerColor = when (event.provider.lowercase(Locale.ROOT)) {
        "bkash" -> BkashColor
        "nagad" -> NagadColor
        "rocket" -> RocketColor
        else -> BrandPurplePrimary
    }

    val (badgeText, badgeBg, badgeTextColor) = when (event.syncStatus) {
        "SENT_APPROVED" -> Triple("Approved (PostgreSQL Verified)", Color(0xFFD1FAE5), BrandSuccess)
        "SENT_UNMATCHED" -> Triple("Unmatched (No Pending Deposit)", Color(0xFFFEF3C7), BrandWarning)
        "SENT_REVIEW" -> Triple("Pending Review", Color(0xFFEDE9FE), BrandPurplePrimary)
        "SENT_DUPLICATE" -> Triple("Duplicate (Already Processed)", Color(0xFFFEE2E2), BrandError)
        "SENDING" -> Triple("Sending to Server...", Color(0xFFE0E7FF), BrandPurpleLight)
        "FAILED_RETRY" -> Triple("Retry Scheduled", Color(0xFFFEE2E2), BrandError)
        else -> Triple("Queued Locally", Color(0xFFF3F4F6), BrandTextSecondary)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Provider Badge + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(providerColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.provider.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIM ${event.simSlot}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = "৳${event.amount}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details: TrxID & Receiver
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TrxID:",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                    )
                    Text(
                        text = event.transactionId,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary
                        )
                    )
                }

                if (event.receiverNumber.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "সেন্ডার / একাউন্ট:",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrandTextTertiary)
                        )
                        val maskedPhone = if (event.receiverNumber.length >= 8) {
                            event.receiverNumber.take(3) + "****" + event.receiverNumber.takeLast(4)
                        } else event.receiverNumber
                        Text(
                            text = maskedPhone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = BrandTextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge & Reason
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    )
                    Text(
                        text = event.receivedAt.take(16).replace("T", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeTextColor.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (!event.backendReason.isNullOrBlank()) {
                Text(
                    text = "কারণ: ${event.backendReason}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandTextSecondary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
        }
    }
}
