package com.example.smsmanager.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsmanager.data.SenderStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(viewModel: SmsViewModel, hasPermissions: Boolean, onRequestPermissions: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var senderToDelete by remember { mutableStateOf<SenderStat?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart SMS Manager") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasPermissions) {
                Button(onClick = onRequestPermissions, modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp)) {
                    Text("Grant SMS Permissions")
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
            TextButton(onClick = { onDeleteClick(sender) }) {
                Text("[X] Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}