package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.util.Log
import com.example.data.repository.PaymentGatewayRepository
import com.example.parser.SmsPaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error reading SMS intent: ${e.message}")
            return
        }

        if (messages.isEmpty()) return

        // Extract SIM Slot / Subscription ID
        val subId = intent.getIntExtra("subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        val simSlot = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            try {
                val subInfo = sm?.getActiveSubscriptionInfo(subId)
                if (subInfo != null) subInfo.simSlotIndex + 1 else 1
            } catch (e: SecurityException) {
                1
            }
        } else {
            intent.getIntExtra("slot", 0) + 1
        }

        // Combine multipart SMS parts if necessary
        val sender = messages[0].displayOriginatingAddress ?: ""
        val fullBody = buildString {
            for (msg in messages) {
                append(msg.displayMessageBody ?: "")
            }
        }
        val timestamp = messages[0].timestampMillis

        Log.d("SmsReceiver", "Incoming SMS from $sender (SIM slot $simSlot)")

        val parsed = SmsPaymentParser.parse(sender, fullBody, timestamp)
        if (parsed != null) {
            Log.i("SmsReceiver", "Matched Payment SMS: ${parsed.provider} ৳${parsed.amount} TrxID: ${parsed.transactionId}")

            val pendingResult = goAsync()
            val repository = PaymentGatewayRepository(context.applicationContext)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = repository.configFlow.first()
                    if (config.isMonitoringActive && config.isConfigured) {
                        repository.queueSmsPayment(parsed, simSlot)
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to queue payment event: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
