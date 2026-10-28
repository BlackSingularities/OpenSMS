package com.blacksingularities.opensms.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kopia zapasowa SMS w pliku JSON (wybranym przez użytkownika). Import pomija wiadomości,
 * które już są w telefonie (ten sam numer, treść i czas), więc można go powtarzać bez duplikatów.
 */
object Backup {
    data class Result(val total: Int, val added: Int)

    fun export(context: Context, target: Uri): Int {
        val list = MessageStore(context).allSms()
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject()
                    .put("address", m.address)
                    .put("body", m.body)
                    .put("date", m.date)
                    .put("date_sent", m.dateSent)
                    .put("type", boxToType(m.box))
                    .put("read", m.read),
            )
        }
        val root = JSONObject()
            .put("format", "opensms-backup")
            .put("version", 1)
            .put("exported", System.currentTimeMillis())
            .put("sms", arr)
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
            ?: error("Nie można zapisać pliku")
        return list.size
    }

    fun import(context: Context, source: Uri, onProgress: (Float) -> Unit = {}): Result {
        val text = context.contentResolver.openInputStream(source)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Nie można odczytać pliku")
        val root = runCatching { JSONObject(text) }.getOrNull() ?: error("To nie jest plik kopii OpenSMS")
        if (root.optString("format") != "opensms-backup") error("To nie jest plik kopii OpenSMS")
        val arr = root.optJSONArray("sms") ?: JSONArray()
        val store = MessageStore(context)
        var added = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val address = o.optString("address")
            val body = o.optString("body")
            val date = o.optLong("date")
            if (address.isBlank() || store.smsExists(address, body, date)) continue
            if (store.insertSmsRaw(address, body, date, o.optLong("date_sent"), o.optInt("type", 1), o.optBoolean("read", true))) added++
            if (i % 50 == 0) onProgress(i.toFloat() / arr.length())
        }
        onProgress(1f)
        return Result(arr.length(), added)
    }

    private fun boxToType(box: Box) = when (box) {
        Box.INBOX -> 1
        Box.SENT -> 2
        Box.DRAFT -> 3
        Box.OUTBOX -> 4
        Box.FAILED -> 5
    }
}
