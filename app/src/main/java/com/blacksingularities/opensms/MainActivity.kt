package com.blacksingularities.opensms

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.blacksingularities.opensms.mms.MmsSender
import com.blacksingularities.opensms.notify.Notifier
import com.blacksingularities.opensms.ui.AppRoot
import com.blacksingularities.opensms.ui.AppViewModel
import com.blacksingularities.opensms.ui.Screen
import com.blacksingularities.opensms.ui.theme.OpenSmsTheme
import com.blacksingularities.opensms.update.UpdateManager

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshDefault()
        if (vm.isDefault) askPermissions()
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        UpdateManager.clearUpdatedNotification(this)
        setContent {
            OpenSmsTheme {
                AppRoot(
                    vm = vm,
                    onRequestDefault = ::requestDefaultRole,
                    onRequestPermissions = { askPermissions() },
                )
            }
        }
        if (savedInstanceState == null) handleIntent(intent)
        if (vm.isDefault) askPermissions(onlyMissing = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onForeground(true)
    }

    override fun onPause() {
        vm.onForeground(false)
        super.onPause()
    }

    private fun requestDefaultRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
            roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
        } else {
            vm.refreshDefault()
        }
    }

    private fun askPermissions(onlyMissing: Boolean = false) {
        val wanted = buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_PHONE_NUMBERS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.RECEIVE_MMS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = wanted.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            if (!onlyMissing) vm.onPermissionsChanged()
            return
        }
        permissionRequest.launch(missing.toTypedArray())
    }

    /** Powiadomienie, smsto:, udostępnianie z innych aplikacji. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val threadId = intent.getLongExtra(Notifier.EXTRA_THREAD, -1)
        if (threadId >= 0) {
            vm.home()
            vm.openThread(threadId)
            return
        }
        when (intent.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                val data = intent.data ?: return
                if (data.scheme !in setOf("sms", "smsto", "mms", "mmsto")) return
                val recipients = parseRecipients(data)
                val body = intent.getStringExtra("sms_body") ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: data.getQueryParameterSafe("body") ?: ""
                vm.home()
                if (recipients.isEmpty()) vm.push(Screen.Compose(text = body)) else vm.openAddresses(recipients, body)
            }
            Intent.ACTION_SEND -> {
                val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT)).joinToString("\n")
                val stream = streamExtra(intent)
                val attachments = listOfNotNull(stream).map { MmsSender.describe(this, it) }
                vm.home()
                vm.push(Screen.Compose(text = text, attachments = attachments))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = streamListExtra(intent)
                vm.home()
                vm.push(Screen.Compose(attachments = uris.take(10).map { MmsSender.describe(this, it) }))
            }
        }
    }

    private fun parseRecipients(data: Uri): List<String> =
        Uri.decode(data.schemeSpecificPart ?: "").substringBefore('?')
            .split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun Uri.getQueryParameterSafe(key: String): String? =
        runCatching { Uri.parse("x://h?" + (schemeSpecificPart?.substringAfter('?', "") ?: "")).getQueryParameter(key) }.getOrNull()

    @Suppress("DEPRECATION")
    private fun streamExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else intent.getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun streamListExtra(intent: Intent): List<Uri> =
        (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) else intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)).orEmpty()
}
