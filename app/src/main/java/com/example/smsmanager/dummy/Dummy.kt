package com.example.smsmanager.dummy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.IBinder

class SmsReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) {} }
class MmsReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) {} }
class HeadlessSmsSendService : Service() { override fun onBind(intent: Intent): IBinder? = null }