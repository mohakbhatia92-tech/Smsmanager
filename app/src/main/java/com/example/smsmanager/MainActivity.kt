package com.example.smsmanager

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smsmanager.data.SmsRepository
import com.example.smsmanager.ui.SmsScreen
import com.example.smsmanager.ui.SmsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SmsRepository(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmsViewModel(repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[SmsViewModel::class.java]

        setContent {
            MaterialTheme {
                var hasRequiredAccess by remember { mutableStateOf(checkAccess()) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // This forces the screen to refresh the moment the permission popup closes
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasRequiredAccess = checkAccess()
                            if (hasRequiredAccess) {
                                viewModel.loadData()
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        if (perms[Manifest.permission.READ_SMS] == true) {
                            requestDefaultSmsRole()
                        }
                    }
                )

                SmsScreen(
                    viewModel = viewModel,
                    hasPermissions = hasRequiredAccess,
                    onRequestPermissions = {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))
                    }
                )
            }
        }
    }

    private fun checkAccess(): Boolean {
        val hasRead = checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
        return hasRead && isDefault
    }

    private fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                @Suppress("DEPRECATION")
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS), 1001)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 1001)
        }
    }
}
