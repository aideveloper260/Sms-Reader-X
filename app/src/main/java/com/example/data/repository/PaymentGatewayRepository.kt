package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClientFactory
import com.example.data.local.AppDatabase
import com.example.data.local.LocalPaymentEvent
import com.example.data.local.PreferencesManager
import com.example.data.local.ProjectConfig
import com.example.data.model.ConsentRequest
import com.example.data.model.HealthResponse
import com.example.data.model.HistoryItem
import com.example.data.model.PaymentEventPayload
import com.example.data.model.PaymentEventResponse
import com.example.data.model.StatsResponse
import com.example.parser.ParsedPaymentSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class PaymentGatewayRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.paymentEventDao()
    private val prefsManager = PreferencesManager(context)

    val configFlow: Flow<ProjectConfig> = prefsManager.projectConfigFlow
    val localEventsFlow: Flow<List<LocalPaymentEvent>> = dao.getAllEvents()

    suspend fun saveProject(
        name: String,
        apiUrl: String,
        deviceKey: String,
        merchantId: String,
        sim: String,
        bkash: Boolean,
        nagad: Boolean,
        rocket: Boolean
    ): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            // Validation: reject PostgreSQL URLs immediately
            val lowerUrl = apiUrl.lowercase(Locale.ROOT)
            if (lowerUrl.startsWith("postgres://") || lowerUrl.startsWith("postgresql://") || lowerUrl.startsWith("jdbc:postgresql://")) {
                return@withContext Result.failure(
                    IllegalArgumentException("PostgreSQL URL গ্রহণযোগ্য নয়। এখানে পাইথন গেটওয়ের HTTPS URL দিন (e.g. https://your-server.render.com).")
                )
            }
            if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
                return@withContext Result.failure(
                    IllegalArgumentException("URL অবশ্যই https:// বা http:// দিয়ে শুরু হতে হবে।")
                )
            }

            val service = ApiClientFactory.createService(apiUrl)
            val authHeader = "Bearer $deviceKey"
            val response = service.checkHealth(authHeader)

            if (response.isSuccessful && response.body() != null) {
                val health = response.body()!!
                prefsManager.saveProject(
                    name = name,
                    apiUrl = apiUrl,
                    deviceKey = deviceKey,
                    merchantId = if (health.merchantId.isNullOrBlank()) merchantId else health.merchantId,
                    sim = sim,
                    bkash = bkash,
                    nagad = nagad,
                    rocket = rocket
                )

                // Record user consent with backend
                val currentConfig = prefsManager.projectConfigFlow.first()
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val consentReq = ConsentRequest(
                        merchantId = currentConfig.merchantId,
                        deviceId = currentConfig.deviceId,
                        consentVersion = "1.0",
                        acceptedAt = sdf.format(Date())
                    )
                    service.recordConsent(authHeader, consentReq)
                } catch (e: Exception) {
                    Log.w("PaymentRepository", "Consent call failed: ${e.message}")
                }

                Result.success(health)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "অথেনটিকেশন ব্যর্থ হয়েছে (Device/Bearer Token ভুল)।"
                    404 -> "API রুট খুঁজে পাওয়া যায়নি। URL যাচাই করুন।"
                    else -> "সার্ভার রেসপন্স কোড: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("সার্ভারের সাথে সংযোগ স্থাপন করা যায়নি: ${e.localizedMessage}"))
        }
    }

    suspend fun queueSmsPayment(
        parsed: ParsedPaymentSms,
        simSlot: Int
    ): LocalPaymentEvent = withContext(Dispatchers.IO) {
        val config = prefsManager.projectConfigFlow.first()

        // Check if provider is enabled in settings
        val isEnabled = when (parsed.provider) {
            "bkash" -> config.enableBkash
            "nagad" -> config.enableNagad
            "rocket" -> config.enableRocket
            else -> false
        }
        if (!isEnabled) {
            throw IllegalStateException("Provider ${parsed.provider} is disabled in project settings.")
        }

        // Check SIM slot restriction
        if (config.selectedSim == "SIM_1" && simSlot != 1) {
            throw IllegalStateException("SIM slot 1 is configured, but SMS received on SIM $simSlot.")
        }
        if (config.selectedSim == "SIM_2" && simSlot != 2) {
            throw IllegalStateException("SIM slot 2 is configured, but SMS received on SIM $simSlot.")
        }

        // Check if event already exists locally (by provider & transactionId)
        val existing = dao.getEventByTrx(parsed.provider, parsed.transactionId)
        if (existing != null) {
            return@withContext existing
        }

        val eventId = UUID.randomUUID().toString()
        val localEvent = LocalPaymentEvent(
            eventId = eventId,
            provider = parsed.provider,
            transactionId = parsed.transactionId,
            amount = parsed.amount,
            receiverNumber = parsed.receiverNumber,
            reference = parsed.reference,
            simSlot = simSlot,
            receivedAt = parsed.timestampIso,
            syncStatus = "QUEUED"
        )
        dao.insertEvent(localEvent)

        // Trigger immediate forward if autoForward is on
        if (config.autoForward && config.isConfigured) {
            forwardEvent(localEvent, config)
        }

        localEvent
    }

    suspend fun forwardEvent(
        event: LocalPaymentEvent,
        config: ProjectConfig? = null
    ): Result<PaymentEventResponse> = withContext(Dispatchers.IO) {
        val activeConfig = config ?: prefsManager.projectConfigFlow.first()
        if (!activeConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Project is not configured."))
        }

        // Update status to SENDING
        dao.updateEvent(event.copy(syncStatus = "SENDING"))

        try {
            val service = ApiClientFactory.createService(activeConfig.apiUrl)
            val authHeader = "Bearer ${activeConfig.deviceKey}"

            val payload = PaymentEventPayload(
                amount = event.amount,
                provider = event.provider,
                transactionId = event.transactionId,
                reference = event.reference,
                receiverNumber = event.receiverNumber,
                currency = event.currency,
                receivedAt = event.receivedAt,
                simSlot = event.simSlot,
                deviceId = activeConfig.deviceId,
                merchantId = activeConfig.merchantId,
                eventId = event.eventId
            )

            val response = service.submitPayment(authHeader, payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newStatus = when (body.status.uppercase(Locale.ROOT)) {
                    "APPROVED" -> "SENT_APPROVED"
                    "UNMATCHED" -> "SENT_UNMATCHED"
                    "PENDING_REVIEW" -> "SENT_REVIEW"
                    "DUPLICATE" -> "SENT_DUPLICATE"
                    else -> "SENT_${body.status}"
                }
                val updatedEvent = event.copy(
                    syncStatus = newStatus,
                    backendReason = body.reason
                )
                dao.updateEvent(updatedEvent)
                Result.success(body)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.errorBody()?.string() ?: "Unknown error"}"
                val retryEvent = event.copy(
                    syncStatus = "FAILED_RETRY",
                    retryCount = event.retryCount + 1,
                    lastError = errorMsg
                )
                dao.updateEvent(retryEvent)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val retryEvent = event.copy(
                syncStatus = "FAILED_RETRY",
                retryCount = event.retryCount + 1,
                lastError = e.localizedMessage
            )
            dao.updateEvent(retryEvent)
            Result.failure(e)
        }
    }

    suspend fun syncPendingQueue(): Int = withContext(Dispatchers.IO) {
        val config = prefsManager.projectConfigFlow.first()
        if (!config.isConfigured) return@withContext 0

        val pending = dao.getPendingEvents(20)
        var successCount = 0
        for (event in pending) {
            val result = forwardEvent(event, config)
            if (result.isSuccess) successCount++
        }
        successCount
    }

    suspend fun fetchDashboardStats(): Result<StatsResponse> = withContext(Dispatchers.IO) {
        val config = prefsManager.projectConfigFlow.first()
        if (!config.isConfigured) {
            return@withContext Result.failure(IllegalStateException("No project configured"))
        }

        try {
            val service = ApiClientFactory.createService(config.apiUrl)
            val auth = "Bearer ${config.deviceKey}"
            val response = service.getStats(auth)
            if (response.isSuccessful && response.body() != null) {
                val stats = response.body()!!
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                prefsManager.updateLastSync(sdf.format(Date()))
                Result.success(stats)
            } else {
                Result.failure(Exception("Stats fetch failed HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHistory(
        provider: String? = null,
        status: String? = null,
        query: String? = null
    ): Result<List<HistoryItem>> = withContext(Dispatchers.IO) {
        val config = prefsManager.projectConfigFlow.first()
        if (!config.isConfigured) {
            return@withContext Result.failure(IllegalStateException("No project configured"))
        }

        try {
            val service = ApiClientFactory.createService(config.apiUrl)
            val auth = "Bearer ${config.deviceKey}"
            val response = service.getPaymentsHistory(
                authHeader = auth,
                provider = provider,
                status = status,
                search = query
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.items)
            } else {
                Result.failure(Exception("History fetch failed HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setMonitoringActive(active: Boolean) {
        prefsManager.updateMonitoringState(active)
    }

    suspend fun setProviders(bkash: Boolean, nagad: Boolean, rocket: Boolean) {
        prefsManager.updateProviders(bkash, nagad, rocket)
    }

    suspend fun setSim(sim: String) {
        prefsManager.updateSim(sim)
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        prefsManager.updateStartOnBoot(enabled)
    }

    suspend fun clearDeliveredCache() {
        dao.clearDeliveredEvents()
    }

    suspend fun removeProject() {
        prefsManager.clearProject()
    }
}
