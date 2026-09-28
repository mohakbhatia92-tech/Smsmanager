package com.example.smsmanager

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsmanager.data.SmsRepository
import com.example.smsmanager.ui.SmsScreen
import com.example.smsmanager.ui.SmsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: SmsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SmsRepository(applicationContext)

        setContent {
            MaterialTheme {
                var hasRequiredAccess by remember { mutableStateOf(checkAccess()) }
                
                val viewModel: SmsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SmsViewModel(repository) as T
                        }
                    }
                )

                // 1. Launcher for Read Contacts/SMS
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    if (perms[Manifest.permission.READ_SMS] == true) {
                        requestDefaultSmsRole { hasRequiredAccess = checkAccess() }
                    }
                }

                // Initial data load if we already have access
                LaunchedEffect(hasRequiredAccess) {
                    if (hasRequiredAccess) {
                        viewModel.loadData()
                    }
                }

                SmsScreen(
                    viewModel = viewModel,
                    hasPermissions = hasRequiredAccess,
                    onRequestPermissions = {
                        permissionsLauncher.launch(arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.READ_CONTACTS
                        ))
                    }
                )
            }
        }
    }

    private fun checkAccess(): Boolean {
        val hasReadPermission = checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            val defaultSmsPackage = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
            defaultSmsPackage == packageName
        }
        
        return hasReadPermission && isDefaultSms
    }

    private fun requestDefaultSmsRole(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult(intent, 1001) 
            }
        } else {
            val intent = android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, 1001)
        }
        onComplete()
    }
}