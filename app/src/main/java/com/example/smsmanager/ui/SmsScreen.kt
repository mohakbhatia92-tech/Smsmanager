package com.example.smsmanager.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsmanager.data.SenderStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    viewModel: SmsViewModel, 
    hasReadSms: Boolean, 
    isDefaultApp: Boolean,
    onRequestPermission: () -> Unit,
    onRequestDefault: () -> Unit,
    onRefresh: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var senderToDelete by remember { mutableStateOf<SenderStat?>(null) }
    
    Scaffold(topBar = { TopAppBar(title = { Text("Smart SMS Manager") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasReadSms || !isDefaultApp) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("App Diagnostics", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(32.dp))
                    
                    Text(if (hasReadSms) "✅ Step 1: SMS Permission Granted" else "❌ Step 1: SMS Permission Missing")
                    if (!hasReadSms) {
                        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 8.dp)) { Text("Grant SMS Permission") }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(if (isDefaultApp) "✅ Step 2: Default App Set" else "❌ Step 2: Not Default SMS App")
                    if (hasReadSms && !isDefaultApp) {
                        Button(onClick = onRequestDefault, modifier = Modifier.padding(top = 8.dp)) { Text("Set as Default App") }
                    }

                    Spacer(Modifier.height(48.dp))
                    OutlinedButton(onClick = onRefresh) { Text("Refresh Status") }
                }
            } else {
                when (val uiState = state) {
                    is SmsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    is SmsUiState.Error -> Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                    is SmsUiState.Success -> {
                        Button(onClick = { viewModel.markAllAsRead() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("✓ Mark All Unread as Read")
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.senders) { sender ->
                                SenderCard(sender = sender, onDeleteClick = { senderToDelete = it })
                            }
                        }
                    }
                }
            }
        }
    }
    senderToDelete?.let { sender ->
        AlertDialog(
            onDismissRequest = { senderToDelete = null },
            title = { Text("Delete Messages") },
            text = { Text("Delete all messages from ${sender.contactName ?: sender.address}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSender(sender.address); senderToDelete = null }) { 
                    Text("Delete", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = { TextButton(onClick = { senderToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SenderCard(sender: SenderStat, onDeleteClick: (SenderStat) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = sender.contactName ?: sender.address, fontWeight = FontWeight.Bold)
                Text(text = "Total: ${sender.totalMessages} • Unread: ${sender.unreadCount}")
                Text(text = "Last active: ${DateUtils.getRelativeTimeSpanString(sender.lastActive)}")
            }
            TextButton(onClick = { onDeleteClick(sender) }) { Text("[X] Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
}
