package com.example.smsmanager

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smsmanager.data.SmsRepository
import com.example.smsmanager.ui.SmsScreen
import com.example.smsmanager.ui.SmsViewModel

class MainActivity : ComponentActivity() {
    
    // We moved this out here so the screen can force-refresh itself at any time
    private var hasRequiredAccess by mutableStateOf(false)
    private lateinit var viewModel: SmsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = SmsRepository(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmsViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[SmsViewModel::class.java]

        val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            hasRequiredAccess = checkAccess()
            if (hasRequiredAccess) viewModel.loadData()
        }

        val permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.READ_SMS] == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val rm = getSystemService(RoleManager::class.java)
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                    } else {
                        hasRequiredAccess = checkAccess()
                    }
                } else {
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    roleLauncher.launch(intent)
                }
            }
        }

        setContent {
            MaterialTheme {
                LaunchedEffect(hasRequiredAccess) {
                    if (hasRequiredAccess) viewModel.loadData()
                }

                SmsScreen(
                    viewModel = viewModel,
                    hasPermissions = hasRequiredAccess,
                    onRequestPermissions = {
                        if (checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = getSystemService(RoleManager::class.java)
                                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                                }
                            } else {
                                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                                roleLauncher.launch(intent)
                            }
                        } else {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))
                        }
                    }
                )
            }
        }
    }

    // THE MAGIC FIX: Automatically checks permissions and refreshes the UI every time a pop-up closes
    override fun onResume() {
        super.onResume()
        hasRequiredAccess = checkAccess()
        if (hasRequiredAccess) {
            viewModel.loadData()
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
}
