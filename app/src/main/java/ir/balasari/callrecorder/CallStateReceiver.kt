package ir.balasari.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val enabled = context.getSharedPreferences(RecordingService.PREFS, Context.MODE_PRIVATE)
            .getBoolean(RecordingService.KEY_ENABLED, false)
        if (!enabled) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val action = when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> RecordingService.ACTION_START
            TelephonyManager.EXTRA_STATE_IDLE -> RecordingService.ACTION_STOP
            else -> return
        }

        try {
            // The foreground service should already have been started by the user
            // from the visible activity. This command only starts/stops the recorder.
            context.startService(Intent(context, RecordingService::class.java).setAction(action))
        } catch (e: Exception) {
            context.getSharedPreferences(RecordingService.PREFS, Context.MODE_PRIVATE)
                .edit().putString(RecordingService.KEY_LAST_ERROR, "ارسال فرمان ضبط ناموفق بود: ${e.javaClass.simpleName}").apply()
        }
    }
}
