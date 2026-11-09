package com.example.parser

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

data class ParsedPaymentSms(
    val provider: String,          // "bkash", "nagad", "rocket"
    val transactionId: String,
    val amount: String,            // "500.00"
    val receiverNumber: String,    // "01XXXXXXXXX" or masked sender/counterpart
    val reference: String = "",
    val timestampIso: String,
    val confidence: Float          // 0.0 to 1.0
)

object SmsPaymentParser {

    // bKash Regex Patterns
    // Example: "You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,250.00. TrxID 9A8B7C6D at 09/11/2026 14:30"
    // Example: "You have received deposit of Tk 1,000.00 ... TrxID 8K1L2M3N"
    private val BKASH_AMOUNT_PATTERN = Pattern.compile(
        "(?:received(?:\\s+deposit\\s+of)?\\s+Tk\\s*|Tk\\s*)([0-9,]+(?:\\.[0-9]{2})?)",
        Pattern.CASE_INSENSITIVE
    )
    private val BKASH_TRX_PATTERN = Pattern.compile(
        "TrxID\\s+([A-Za-z0-9_-]{4,30})",
        Pattern.CASE_INSENSITIVE
    )
    private val SENDER_NUM_PATTERN = Pattern.compile(
        "(?:from\\s+|A/C:\\s*|sender:\\s*)(\\+?8801[3-9][0-9]{8}|01[3-9][0-9]{8})",
        Pattern.CASE_INSENSITIVE
    )

    // Nagad Regex Patterns
    // Example: "You have received Tk 250.00 from 01812345678. TxnID: 76AB8C9D. Balance: Tk 1,500.00"
    private val NAGAD_TRX_PATTERN = Pattern.compile(
        "(?:TxnID|Txn\\s*Id|TrxID):?\\s*([A-Za-z0-9_-]{4,30})",
        Pattern.CASE_INSENSITIVE
    )

    // Rocket Regex Patterns
    // Example: "Tk500.00 received from A/C: 01912345678. TxnId: 109283746. Fee Tk 0.00"
    private val ROCKET_AMOUNT_PATTERN = Pattern.compile(
        "Tk\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)\\s*received",
        Pattern.CASE_INSENSITIVE
    )
    private val ROCKET_TRX_PATTERN = Pattern.compile(
        "(?:TxnId|TxnID|TxId):?\\s*([A-Za-z0-9_-]{4,30})",
        Pattern.CASE_INSENSITIVE
    )

    // Security Filter: Detect and reject OTPs, passwords, PINs, or personal messages
    private val OTP_FILTER = Pattern.compile(
        "(?:verification\\s+code|security\\s+code|one\\s*time\\s*password|OTP|PIN\\b|password\\b|code\\s+is\\b)",
        Pattern.CASE_INSENSITIVE
    )

    // Reject outgoing payment SMS (e.g., "You have transferred Tk", "Send Money to ... successful")
    private val OUTGOING_FILTER = Pattern.compile(
        "(?:sent\\s+Tk|transferred\\s+Tk|send\\s+money\\s+to|payment\\s+to|cash\\s*out|mobile\\s+recharge)",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(sender: String, messageBody: String, timestampMillis: Long = System.currentTimeMillis()): ParsedPaymentSms? {
        val trimmed = messageBody.trim()

        // 1. Never forward OTP or security codes
        if (OTP_FILTER.matcher(trimmed).find()) {
            return null
        }

        // 2. Reject outgoing money SMS
        if (OUTGOING_FILTER.matcher(trimmed).find()) {
            return null
        }

        val lowerSender = sender.lowercase(Locale.ROOT)
        val lowerBody = trimmed.lowercase(Locale.ROOT)

        // Identify provider from sender or body signature
        val provider = when {
            lowerSender.contains("bkash") || lowerBody.contains("bkash") -> "bkash"
            lowerSender.contains("nagad") || lowerBody.contains("nagad") || lowerSender.contains("16167") -> "nagad"
            lowerSender.contains("rocket") || lowerBody.contains("rocket") || lowerSender.contains("16216") -> "rocket"
            else -> {
                // If sender isn't standard alphanumeric, check strict body keywords
                if (lowerBody.contains("trxid") && lowerBody.contains("received tk")) "bkash"
                else if (lowerBody.contains("txnid") && lowerBody.contains("nagad")) "nagad"
                else null
            }
        } ?: return null

        val (trxId, rawAmount, counterpart) = when (provider) {
            "bkash" -> extractBkash(trimmed)
            "nagad" -> extractNagad(trimmed)
            "rocket" -> extractRocket(trimmed)
            else -> Triple(null, null, null)
        }

        if (trxId.isNullOrBlank() || rawAmount.isNullOrBlank()) {
            return null
        }

        // Clean & normalize money amount to exact two decimal places (e.g. 500.00)
        val cleanAmount = try {
            val normalizedNum = rawAmount.replace(",", "").trim()
            val bd = BigDecimal(normalizedNum).setScale(2, RoundingMode.HALF_UP)
            if (bd <= BigDecimal.ZERO || bd > BigDecimal("1000000.00")) return null
            bd.toPlainString()
        } catch (e: Exception) {
            return null
        }

        // Normalize Counterpart / Receiver Number (BD standard: 01XXXXXXXXX)
        val normalizedReceiver = normalizeBdPhone(counterpart ?: "")

        val isoTimestamp = formatToIso(timestampMillis)

        return ParsedPaymentSms(
            provider = provider,
            transactionId = trxId.uppercase(Locale.ROOT),
            amount = cleanAmount,
            receiverNumber = normalizedReceiver,
            reference = counterpart ?: "",
            timestampIso = isoTimestamp,
            confidence = 0.95f
        )
    }

    private fun extractBkash(body: String): Triple<String?, String?, String?> {
        val trxMatcher = BKASH_TRX_PATTERN.matcher(body)
        val trx = if (trxMatcher.find()) trxMatcher.group(1) else null

        val amtMatcher = BKASH_AMOUNT_PATTERN.matcher(body)
        val amt = if (amtMatcher.find()) amtMatcher.group(1) else null

        val phoneMatcher = SENDER_NUM_PATTERN.matcher(body)
        val phone = if (phoneMatcher.find()) phoneMatcher.group(1) else null

        return Triple(trx, amt, phone)
    }

    private fun extractNagad(body: String): Triple<String?, String?, String?> {
        val trxMatcher = NAGAD_TRX_PATTERN.matcher(body)
        val trx = if (trxMatcher.find()) trxMatcher.group(1) else null

        val amtMatcher = BKASH_AMOUNT_PATTERN.matcher(body)
        val amt = if (amtMatcher.find()) amtMatcher.group(1) else null

        val phoneMatcher = SENDER_NUM_PATTERN.matcher(body)
        val phone = if (phoneMatcher.find()) phoneMatcher.group(1) else null

        return Triple(trx, amt, phone)
    }

    private fun extractRocket(body: String): Triple<String?, String?, String?> {
        val trxMatcher = ROCKET_TRX_PATTERN.matcher(body)
        val trx = if (trxMatcher.find()) trxMatcher.group(1) else null

        var amtMatcher = ROCKET_AMOUNT_PATTERN.matcher(body)
        val amt = if (amtMatcher.find()) {
            amtMatcher.group(1)
        } else {
            val fallback = BKASH_AMOUNT_PATTERN.matcher(body)
            if (fallback.find()) fallback.group(1) else null
        }

        val phoneMatcher = SENDER_NUM_PATTERN.matcher(body)
        val phone = if (phoneMatcher.find()) phoneMatcher.group(1) else null

        return Triple(trx, amt, phone)
    }

    private fun normalizeBdPhone(raw: String): String {
        var num = raw.trim().replace(" ", "").replace("-", "")
        if (num.startsWith("+880")) num = num.substring(3)
        if (num.startsWith("880")) num = num.substring(2)
        return num
    }

    private fun formatToIso(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestampMillis))
    }
}
