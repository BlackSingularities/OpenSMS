package com.blacksingularities.opensms.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

/** Karty SIM (dual SIM) i menedżer SMS dla wybranej karty. */
object Sims {
    private fun hasPhoneState(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun active(context: Context): List<SimCard> {
        if (!hasPhoneState(context)) return emptyList()
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            sm.activeSubscriptionInfoList.orEmpty().map { info ->
                SimCard(
                    subId = info.subscriptionId,
                    slot = info.simSlotIndex,
                    name = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${info.simSlotIndex + 1}",
                    number = ownNumber(context, sm, info.subscriptionId),
                    color = info.iconTint,
                )
            }.sortedBy { it.slot }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun ownNumber(context: Context, sm: SubscriptionManager, subId: Int): String? = runCatching {
        val n = if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) sm.getPhoneNumber(subId) else null
        } else {
            sm.getActiveSubscriptionInfo(subId)?.number
        }
        n?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Własne numery (do pominięcia siebie w rozmowach grupowych MMS). */
    fun ownNumbers(context: Context): Set<String> = active(context).mapNotNull { it.number?.let(::normalizeNumber) }.toSet()

    fun defaultSmsSubId(): Int = SubscriptionManager.getDefaultSmsSubscriptionId()

    @Suppress("DEPRECATION")
    fun smsManager(context: Context, subId: Int): SmsManager {
        val valid = subId >= 0 && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
        return if (Build.VERSION.SDK_INT >= 31) {
            val base = context.getSystemService(SmsManager::class.java)
            if (valid) base.createForSubscriptionId(subId) else base
        } else {
            if (valid) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }
    }
}
