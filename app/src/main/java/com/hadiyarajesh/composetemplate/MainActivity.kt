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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    if (perms[Manifest.permission.READ_SMS] == true) {
                        requestDefaultSmsRole { hasRequiredAccess = checkAccess() }
                    }
                }

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
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            defaultSmsPackage == packageName
        }
        
        return hasReadPermission && isDefaultSms
    }

    private fun requestDefaultSmsRole(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                @Suppress("DEPRECATION")
                startActivityForResult(intent, 1001) 
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 1001)
        }
        onComplete()
    }
}
