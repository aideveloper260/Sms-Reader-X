package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.PaymentGatewayRepository
import com.example.service.SmsMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = PaymentGatewayRepository(context.applicationContext)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = repository.configFlow.first()
                    if (config.startOnBoot && config.isConfigured && config.isMonitoringActive) {
                        SmsMonitorService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
