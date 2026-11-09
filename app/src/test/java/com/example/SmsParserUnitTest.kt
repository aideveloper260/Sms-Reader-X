package com.example

import com.example.parser.SmsPaymentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserUnitTest {

    @Test
    fun testBkashIncomingPaymentSmsParsing() {
        val sender = "bKash"
        val body = "You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,250.00. TrxID 9A8B7C6D at 09/11/2026 14:30"
        val parsed = SmsPaymentParser.parse(sender, body)

        assertNotNull("Should parse valid bKash SMS", parsed)
        assertEquals("bkash", parsed?.provider)
        assertEquals("500.00", parsed?.amount)
        assertEquals("9A8B7C6D", parsed?.transactionId)
        assertEquals("01712345678", parsed?.receiverNumber)
    }

    @Test
    fun testNagadIncomingPaymentSmsParsing() {
        val sender = "16167"
        val body = "You have received Tk 250.00 from 01812345678. TxnID: 76AB8C9D. Balance: Tk 1,500.00"
        val parsed = SmsPaymentParser.parse(sender, body)

        assertNotNull("Should parse valid Nagad SMS", parsed)
        assertEquals("nagad", parsed?.provider)
        assertEquals("250.00", parsed?.amount)
        assertEquals("76AB8C9D", parsed?.transactionId)
        assertEquals("01812345678", parsed?.receiverNumber)
    }

    @Test
    fun testRocketIncomingPaymentSmsParsing() {
        val sender = "16216"
        val body = "Tk500.00 received from A/C: 01912345678. TxnId: 109283746. Fee Tk 0.00"
        val parsed = SmsPaymentParser.parse(sender, body)

        assertNotNull("Should parse valid Rocket SMS", parsed)
        assertEquals("rocket", parsed?.provider)
        assertEquals("500.00", parsed?.amount)
        assertEquals("109283746", parsed?.transactionId)
        assertEquals("01912345678", parsed?.receiverNumber)
    }

    @Test
    fun testOtpMessageIsRejected() {
        val sender = "bKash"
        val body = "Your bKash verification code is 492018. Do not share your OTP or PIN with anyone."
        val parsed = SmsPaymentParser.parse(sender, body)

        assertNull("OTP SMS must be strictly rejected", parsed)
    }

    @Test
    fun testOutgoingSendMoneyIsRejected() {
        val sender = "bKash"
        val body = "Send Money to 01799887766 Tk 500.00 successful. Fee Tk 5.00. Balance Tk 1,000.00. TrxID 8A9B0C1D"
        val parsed = SmsPaymentParser.parse(sender, body)

        assertNull("Outgoing transfer SMS must be strictly rejected", parsed)
    }
}
