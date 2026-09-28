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
            val selection = "${Telephony.Sms.READ} = 0"
            val updatedCount = context.contentResolver.update(
                Telephony.Sms.Inbox.CONTENT_URI,
                values,
                selection,
                null
            )
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessagesBySender(address: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val selection = "${Telephony.Sms.ADDRESS} = ?"
            val deletedCount = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                selection,
                arrayOf(address)
            )
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFrequentSenders(daysInt: Int? = null): List<SenderStat> = withContext(Dispatchers.IO) {
        val statsMap = mutableMapOf<String, SenderStat>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (daysInt != null) {
            val cutoff = System.currentTimeMillis() - (daysInt * 24L * 60L * 60L * 1000L)
            selection = "${Telephony.Sms.DATE} >= ?"
            selectionArgs = arrayOf(cutoff.toString())
        }

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
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
                    statsMap[address] = SenderStat(
                        address = address,
                        contactName = getContactName(address),
                        totalMessages = 1,
                        unreadCount = if (isRead) 0 else 1,
                        lastActive = date
                    )
                }
            }
        }

        // Sort by volume descending
        statsMap.values.sortedByDescending { it.totalMessages }
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }
}