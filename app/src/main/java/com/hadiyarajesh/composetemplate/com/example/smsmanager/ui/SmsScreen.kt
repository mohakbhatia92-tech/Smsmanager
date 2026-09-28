package com.example.smsmanager.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
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
fun SmsScreen(
    viewModel: SmsViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var senderToDelete by remember { mutableStateOf<SenderStat?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart SMS Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasPermissions) {
                PermissionRationale(onRequestPermissions)
            } else {
                when (val uiState = state) {
                    is SmsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    is SmsUiState.Error -> Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                    is SmsUiState.Success -> {
                        Button(
                            onClick = { viewModel.markAllAsRead() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mark All Unread as Read")
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.senders) { sender ->
                                SenderCard(
                                    sender = sender,
                                    onDeleteClick = { senderToDelete = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    senderToDelete?.let { sender ->
        AlertDialog(
            onDismissRequest = { senderToDelete = null },
            title = { Text("Delete Messages") },
            text = { Text("Are you sure you want to delete all ${sender.totalMessages} messages from ${sender.contactName ?: sender.address}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSender(sender.address)
                        senderToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { senderToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SenderCard(sender: SenderStat, onDeleteClick: (SenderStat) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender.contactName ?: sender.address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${sender.totalMessages} • Unread: ${sender.unreadCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sender.unreadCount > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = "Last active: ${DateUtils.getRelativeTimeSpanString(sender.lastActive)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = { onDeleteClick(sender) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PermissionRationale(onRequestClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "To analyze, clean, and manage your inbox locally on this device, this app requires access to read SMS and must be set as your Default SMS App to perform deletions safely.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestClick) {
            Text("Grant Permissions & Setup")
        }
    }
}