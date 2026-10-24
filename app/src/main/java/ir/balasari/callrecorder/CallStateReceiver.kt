package ir.balasari.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val service = Intent(context, RecordingService::class.java).apply {
            action = if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) RecordingService.ACTION_START else if (state == TelephonyManager.EXTRA_STATE_IDLE) RecordingService.ACTION_STOP else return
        }
        try { context.startForegroundService(service) } catch (_: Exception) { /* Android may block background microphone service starts. */ }
    }
}
