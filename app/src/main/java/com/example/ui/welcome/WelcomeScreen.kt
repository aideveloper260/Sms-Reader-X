package com.example.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BrandFooter
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPurpleDark
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandTextSecondary

@Composable
fun WelcomeScreen(
    onAddProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 24.dp)
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Center Content: Logo, Title, Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing purple logo container
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .shadow(16.dp, CircleShape, spotColor = BrandPurplePrimary)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandPurpleLight, BrandPurpleDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sms_reader_logo_1794201960642),
                    contentDescription = "SMS Reader X Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SMS READER X",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = BrandTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "আপনার পেমেন্ট SMS নিরাপদে পরিচালনা করুন",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandTextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Trust highlights card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FeatureRow(
                        icon = Icons.Default.Security,
                        title = "সম্পূর্ণ নিরাপদ ও লোকাল",
                        desc = "কোনো ব্যাংক পাসওয়ার্ড বা OTP রিড করা হয় না"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FeatureRow(
                        icon = Icons.Default.Bolt,
                        title = "স্বয়ংক্রিয় পেমেন্ট ম্যাচিং",
                        desc = "bKash, Nagad ও Rocket ডিপোজিট তাত্ক্ষণিক ভেরিফিকেশন"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FeatureRow(
                        icon = Icons.Default.CheckCircle,
                        title = "টেলিগ্রাম বটের সাথে সিঙ্ক",
                        desc = "রিয়েল-টাইম ব্যালেন্স ক্রেডিট ও ডুप्लिकিক চেক"
                    )
                }
            }
        }

        // Bottom CTA Button & Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onAddProjectClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPurplePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("add_project_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "প্রজেক্ট যোগ করুন",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BrandFooter()
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandPurplePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BrandTextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
