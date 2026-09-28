package com.example.smsmanager.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SenderStat(
    val address: String,
    val contactName: String?,
    val totalMessages: Int,
    val unreadCount: Int,
    val lastActive: Long
)

class SmsRepository(private val context: Context) {
    suspend fun markAllAsRead(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            val updatedCount = context.contentResolver.update(Telephony.Sms.Inbox.CONTENT_URI, values, "${Telephony.Sms.READ} = 0", null)
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteMessagesBySender(address: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedCount = context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "${Telephony.Sms.ADDRESS} = ?", arrayOf(address))
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getFrequentSenders(daysInt: Int? = null): List<SenderStat> = withContext(Dispatchers.IO) {
        val statsMap = mutableMapOf<String, SenderStat>()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.READ)
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        if (daysInt != null) {
            selection = "${Telephony.Sms.DATE} >= ?"
            selectionArgs = arrayOf((System.currentTimeMillis() - (daysInt * 24L * 60L * 60L * 1000L)).toString())
        }
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIdx) ?: continue
                val date = cursor.getLong(dateIdx)
                val isRead = cursor.getInt(readIdx) == 1
                val existing = statsMap[address]
                if (existing != null) {
                    statsMap[address] = existing.copy(
                        totalMessages = existing.totalMessages + 1,
                        unreadCount = existing.unreadCount + if (isRead) 0 else 1,
                        lastActive = maxOf(existing.lastActive, date)
                    )
                } else {
                    statsMap[address] = SenderStat(address, getContactName(address), 1, if (isRead) 0 else 1, date)
                }
            }
        }
        statsMap.values.sortedByDescending { it.totalMessages }
    }
    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }
}