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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smsmanager.data.SmsRepository
import com.example.smsmanager.ui.SmsScreen
import com.example.smsmanager.ui.SmsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                var hasReadSms by remember { mutableStateOf(checkReadPermission()) }
                var isDefaultApp by remember { mutableStateOf(checkDefaultApp()) }
                val coroutineScope = rememberCoroutineScope()

                val updateState = {
                    hasReadSms = checkReadPermission()
                    isDefaultApp = checkDefaultApp()
                    if (hasReadSms && isDefaultApp) viewModel.loadData()
                }

                val roleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { coroutineScope.launch { delay(500); updateState() } }
                )

                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { updateState() }
                )

                LaunchedEffect(hasReadSms, isDefaultApp) {
                    if (hasReadSms && isDefaultApp) viewModel.loadData()
                }

                SmsScreen(
                    viewModel = viewModel,
                    hasReadSms = hasReadSms,
                    isDefaultApp = isDefaultApp,
                    onRequestPermission = {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))
                    },
                    onRequestDefault = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val rm = getSystemService(RoleManager::class.java)
                            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
                                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                            }
                        } else {
                            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                            roleLauncher.launch(intent)
                        }
                    },
                    onRefresh = { updateState() }
                )
            }
        }
    }

    private fun checkReadPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun checkDefaultApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }
}
