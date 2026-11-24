package com.blacksingularities.opensms.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.ScheduledMessage
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.mms.MmsDownloader
import com.blacksingularities.opensms.notify.Notifier
import com.blacksingularities.opensms.sms.MessageSender
import com.blacksingularities.opensms.sms.async

/** Wiadomości zaplanowane: dokładny alarm (gdy system pozwala) i wysyłka o czasie. */
object Scheduler {
    private const val EXTRA_ID = "id"
    const val ACTION_SEND = "com.blacksingularities.opensms.SCHEDULED_SEND"
    const val ACTION_SKIP = "com.blacksingularities.opensms.SCHEDULED_SKIP"

    fun add(context: Context, message: ScheduledMessage) {
        val s = Settings.get(context)
        s.saveScheduled(s.scheduled.filter { it.id != message.id } + message)
        arm(context, message)
    }

    fun cancel(context: Context, id: Long) {
        val s = Settings.get(context)
        s.saveScheduled(s.scheduled.filter { it.id != id })
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, id))
        Notifier.cancelScheduledConfirm(context, id)
    }

    /** Po restarcie telefonu i zmianie uprawnień alarmy trzeba ustawić od nowa. */
    fun rearmAll(context: Context) {
        Settings.get(context).reloadScheduled().forEach { arm(context, it) }
    }

    fun canExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    private fun arm(context: Context, m: ScheduledMessage) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(context, m.id)
        val at = m.time.coerceAtLeast(System.currentTimeMillis() + 1000)
        if (canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pending(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, ScheduledSendReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Alarm o czasie: albo wysyła od razu, albo (z potwierdzeniem) pyta w powiadomieniu. */
    internal fun fire(context: Context, id: Long) {
        val m = Settings.get(context).reloadScheduled().firstOrNull { it.id == id } ?: return
        if (m.confirm) {
            Notifier.showScheduledConfirm(context, m, Contacts.title(context, m.addresses))
            return
        }
        sendNow(context, id)
    }

    /** Wysyłka teraz – z alarmu albo po „Wyślij” w powiadomieniu. */
    internal fun sendNow(context: Context, id: Long) {
        Notifier.cancelScheduledConfirm(context, id)
        val s = Settings.get(context)
        val m = s.reloadScheduled().firstOrNull { it.id == id } ?: return
        s.saveScheduled(s.scheduled.filter { it.id != id })
        val result = runCatching { MessageSender.send(context, m.addresses, m.text, subId = m.subId) }.getOrNull() ?: return
        Notifier.showScheduledSent(context, result.threadId, Contacts.title(context, m.addresses))
    }
}

class ScheduledSendReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1)
        if (id < 0) return
        when (intent.action) {
            Scheduler.ACTION_SEND -> async { Scheduler.sendNow(context, id) }
            Scheduler.ACTION_SKIP -> async {
                Notifier.cancelScheduledConfirm(context, id)
                Scheduler.cancel(context, id)
            }
            else -> async { Scheduler.fire(context, id) }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        async {
            Scheduler.rearmAll(context)
            MmsDownloader.cleanup(context)
        }
    }
}
