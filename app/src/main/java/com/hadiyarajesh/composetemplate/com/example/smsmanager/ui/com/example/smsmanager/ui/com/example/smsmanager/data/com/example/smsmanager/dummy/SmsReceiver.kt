package com.example.smsmanager.dummy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}

class HeadlessSmsSendService : android.app.Service() {
    override fun onBind(intent: Intent): android.os.IBinder? = null
}