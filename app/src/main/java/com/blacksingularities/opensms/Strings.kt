package com.blacksingularities.opensms

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes

/**
 * Dostęp do napisów z zasobów spoza Compose (odbiorniki, powiadomienia, ViewModel).
 * Kontekst aplikacji ustawia [OpenSmsApp]; zasoby podążają za językiem systemu.
 */
@SuppressLint("StaticFieldLeak")
object AppStrings {
    lateinit var context: Context
}

fun tr(@StringRes id: Int, vararg args: Any?): String =
    if (args.isEmpty()) AppStrings.context.getString(id) else AppStrings.context.getString(id, *args)
