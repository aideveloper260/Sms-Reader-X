package com.example.ui.project

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PaymentGatewayRepository
import com.example.ui.BrandFooter
import com.example.ui.theme.BkashColor
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandErrorContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandTextSecondary
import com.example.ui.theme.NagadColor
import com.example.ui.theme.RocketColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectScreen(
    repository: PaymentGatewayRepository,
    onProjectSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("My Payment Project") }
    var apiUrl by remember { mutableStateOf("") }
    var deviceKey by remember { mutableStateOf("") }
    var merchantId by remember { mutableStateOf("MERCHANT001") }
    var isKeyVisible by remember { mutableStateOf(false) }

    var selectedSim by remember { mutableStateOf("ALL") } // "ALL", "SIM_1", "SIM_2"

    var enableBkash by remember { mutableStateOf(true) }
    var enableNagad by remember { mutableStateOf(true) }
    var enableRocket by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val allProvidersChecked = enableBkash && enableNagad && enableRocket

    fun validateAndSave() {
        errorMessage = null
        successMessage = null

        if (projectName.isBlank()) {
            errorMessage = "প্রজেক্টের নাম আবশ্যক।"
            return
        }

        val trimmedUrl = apiUrl.trim()
        val lowerUrl = trimmedUrl.lowercase()

        // Strict rejection of raw PostgreSQL connections as per master prompt
        if (lowerUrl.startsWith("postgres://") || lowerUrl.startsWith("postgresql://") || lowerUrl.startsWith("jdbc:postgresql://")) {
            errorMessage = "PostgreSQL URL এখানে গ্রহণযোগ্য নয়! এটি আপনার Python Gateway-এর গোপন ডাটাবেজ তথ্য। অনুগ্রহ করে পাইথন সার্ভারের HTTPS API URL দিন (যেমন: https://your-domain.render.com)।"
            return
        }

        if (!lowerUrl.startsWith("https://") && !lowerUrl.startsWith("http://")) {
            errorMessage = "সঠিক HTTPS API URL প্রদান করুন (e.g. https://your-server.com)।"
            return
        }

        if (deviceKey.isBlank()) {
            errorMessage = "Device / Project Key (Bearer Token) প্রদান করুন।"
            return
        }

        if (!enableBkash && !enableNagad && !enableRocket) {
            errorMessage = "অন্তত একটি পেমেন্ট প্রোভাইডার সিলেক্ট করুন।"
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = repository.saveProject(
                name = projectName.trim(),
                apiUrl = trimmedUrl,
                deviceKey = deviceKey.trim(),
                merchantId = merchantId.trim(),
                sim = selectedSim,
                bkash = enableBkash,
                nagad = enableNagad,
                rocket = enableRocket
            )

            isLoading = false
            result.onSuccess {
                successMessage = "কানেকশন সফল হয়েছে! প্রজেক্ট সক্রিয়।"
                onProjectSaved()
            }.onFailure { err ->
                errorMessage = err.message ?: "কানেকশন ব্যর্থ হয়েছে।"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "প্রজেক্ট যোগ করুন",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBackground
                )
            )
        },
        containerColor = BrandBackground,
        modifier = modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("add_project_form")
        ) {
            // Card 1: Project Details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "বেসিক তথ্য",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Project Name
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("১. প্রজেক্টের নাম *") },
                        placeholder = { Text("My Payment Project") },
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null, tint = BrandPurplePrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Backend API URL
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("২. Backend API URL *") },
                        placeholder = { Text("https://your-server.render.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = BrandPurplePrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_url_input")
                    )
                    Text(
                        text = "এখানে আপনার Python Gateway-এর HTTPS URL দিন। PostgreSQL URL এখানে দেবেন না।",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandTextSecondary,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Device / Project Key
                    OutlinedTextField(
                        value = deviceKey,
                        onValueChange = { deviceKey = it },
                        label = { Text("৩. Device / Project Key *") },
                        placeholder = { Text("GATEWAY_BEARER_TOKEN") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = BrandPurplePrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key Visibility"
                                )
                            }
                        },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_key_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Merchant ID
                    OutlinedTextField(
                        value = merchantId,
                        onValueChange = { merchantId = it },
                        label = { Text("৪. Merchant ID") },
                        placeholder = { Text("MERCHANT001") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: SIM Selection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SimCard, contentDescription = null, tint = BrandPurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কোন SIM-এ SMS আসবে?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SimOption(
                            label = "সব SIM",
                            selected = selectedSim == "ALL",
                            onSelect = { selectedSim = "ALL" }
                        )
                        SimOption(
                            label = "SIM 1",
                            selected = selectedSim == "SIM_1",
                            onSelect = { selectedSim = "SIM_1" }
                        )
                        SimOption(
                            label = "SIM 2",
                            selected = selectedSim == "SIM_2",
                            onSelect = { selectedSim = "SIM_2" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Providers
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "কোন পেমেন্ট? (এক বা একাধিক সিলেক্ট করুন)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Select All Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val target = !allProvidersChecked
                                enableBkash = target
                                enableNagad = target
                                enableRocket = target
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = allProvidersChecked,
                            onCheckedChange = { checked ->
                                enableBkash = checked
                                enableNagad = checked
                                enableRocket = checked
                            },
                            colors = CheckboxDefaults.colors(checkedColor = BrandPurplePrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সব (bKash, Nagad, Rocket)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // bKash
                    ProviderCheckRow(
                        name = "bKash",
                        color = BkashColor,
                        checked = enableBkash,
                        onCheckedChange = { enableBkash = it }
                    )

                    // Nagad
                    ProviderCheckRow(
                        name = "Nagad",
                        color = NagadColor,
                        checked = enableNagad,
                        onCheckedChange = { enableNagad = it }
                    )

                    // Rocket
                    ProviderCheckRow(
                        name = "Rocket",
                        color = RocketColor,
                        checked = enableRocket,
                        onCheckedChange = { enableRocket = it }
                    )
                }
            }

            // Error or Success Banner
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandErrorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = BrandError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrandError,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("বাতিল", color = BrandTextSecondary)
                }

                Button(
                    onClick = { validateAndSave() },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurplePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_project_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সংরক্ষণ",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            BrandFooter()
        }
    }
}

@Composable
private fun SimOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onSelect() }
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = BrandPurplePrimary)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) BrandPurplePrimary else BrandTextPrimary
            )
        )
    }
}

@Composable
private fun ProviderCheckRow(
    name: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = BrandTextPrimary
            )
        )
    }
}
