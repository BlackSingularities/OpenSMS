package com.blacksingularities.opensms

import com.blacksingularities.opensms.mms.Pdu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PduTest {
    @Test
    fun sendReqRoundTrip() {
        val parts = listOf(
            Pdu.Part("application/smil", "<smil/>".toByteArray(), name = "smil.xml", contentId = "smil", contentLocation = "smil.xml"),
            Pdu.Part("image/jpeg", ByteArray(5000) { it.toByte() }, name = "foto.jpg", contentId = "foto.jpg", contentLocation = "foto.jpg"),
            Pdu.Part("text/plain", "Zażółć gęślą jaźń".toByteArray(), name = "text_0.txt", contentId = "text_0.txt", contentLocation = "text_0.txt", charset = 106),
        )
        val bytes = Pdu.encodeSendReq("T123", listOf("+48600100200", "jan@example.com"), "Temat ąę", parts, deliveryReport = true)
        val p = Pdu.parse(bytes)
        assertEquals(Pdu.SEND_REQ, p.messageType)
        assertEquals("T123", p.transactionId)
        assertEquals(listOf("+48600100200", "jan@example.com"), p.to)
        assertEquals("Temat ąę", p.subject)
        assertEquals(Pdu.MULTIPART_RELATED, p.contentType)
        assertEquals(3, p.parts.size)
        assertEquals("image/jpeg", p.parts[1].contentType)
        assertEquals(5000, p.parts[1].data.size)
        assertEquals("foto.jpg", p.parts[1].contentId)
        assertEquals("text/plain", p.parts[2].contentType)
        assertEquals("Zażółć gęślą jaźń", p.parts[2].text)
    }

    @Test
    fun notificationInd() {
        // m-notification-ind: typ, id transakcji, wersja, From, klasa, rozmiar, ważność, adres treści.
        val b = java.io.ByteArrayOutputStream()
        b.write(0x8C); b.write(0x82)
        b.write(0x98); b.write("abc".toByteArray()); b.write(0)
        b.write(0x8D); b.write(0x92)
        val from = "+48500600700/TYPE=PLMN".toByteArray()
        b.write(0x89); b.write(from.size + 2); b.write(0x80); b.write(from); b.write(0)
        b.write(0x8A); b.write(0x80)
        b.write(0x8E); b.write(2); b.write(0x30); b.write(0x39)
        b.write(0x88); b.write(3); b.write(0x81); b.write(1); b.write(60)
        b.write(0x83); b.write("http://mms.example/x1".toByteArray()); b.write(0)
        val p = Pdu.parse(b.toByteArray())
        assertEquals(Pdu.NOTIFICATION_IND, p.messageType)
        assertEquals("abc", p.transactionId)
        assertEquals("+48500600700", p.from)
        assertEquals(0x3039L, p.messageSize)
        assertEquals("http://mms.example/x1", p.contentLocation)
        assertNotNull(p.expiry)
    }

    @Test
    fun acknowledge() {
        val p = Pdu.parse(Pdu.encodeAcknowledgeInd("xyz"))
        assertEquals(Pdu.ACKNOWLEDGE_IND, p.messageType)
        assertEquals("xyz", p.transactionId)
    }
}
