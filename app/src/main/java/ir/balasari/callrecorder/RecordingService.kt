package ir.balasari.callrecorder

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        when (intent?.action) {
            ACTION_ENABLE -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(KEY_ENABLED, true).remove(KEY_LAST_ERROR).apply()
                startForeground(NOTIFICATION_ID, buildNotification("ضبط خودکار فعال است؛ منتظر تماس"))
            }
            ACTION_START -> {
                // For a service started from the activity, ensure foreground state first.
                startForeground(NOTIFICATION_ID, buildNotification("تماس شناسایی شد؛ تلاش برای ضبط"))
                startRecording()
            }
            ACTION_STOP -> {
                stopRecording()
                if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false)) {
                    startForeground(NOTIFICATION_ID, buildNotification("ضبط خودکار فعال است؛ منتظر تماس"))
                }
            }
            ACTION_DISABLE -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(KEY_ENABLED, false).apply()
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Do not silently keep a service alive if it was started without an action.
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return
        try {
            val dir = File(getExternalFilesDir(null), "ضبط-مکالمه").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "Call_$stamp.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            output = file
            recorder = r
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_LAST_ERROR).apply()
            updateNotification("ضبط تماس در حال انجام است")
        } catch (e: Exception) {
            try { output?.delete() } catch (_: Exception) {}
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            output = null
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_LAST_ERROR, "شروع ضبط ناموفق بود: ${e.javaClass.simpleName}").apply()
            updateNotification("شروع ضبط ناموفق بود؛ مجوز میکروفون و اعلان‌ها را بررسی کنید")
        }
    }

    private fun stopRecording() {
        val active = recorder
        try {
            if (active != null) active.stop()
        } catch (_: Exception) {
            try { output?.delete() } catch (_: Exception) {}
        } finally {
            try { active?.release() } catch (_: Exception) {}
            recorder = null
            output = null
        }
    }

    private fun buildNotification(message: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("ضبط مکالمه")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "وضعیت ضبط مکالمه", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    companion object {
        const val ACTION_ENABLE = "ir.balasari.callrecorder.ENABLE"
        const val ACTION_DISABLE = "ir.balasari.callrecorder.DISABLE"
        const val ACTION_START = "ir.balasari.callrecorder.START"
        const val ACTION_STOP = "ir.balasari.callrecorder.STOP"
        const val PREFS = "call_recorder_settings"
        const val KEY_ENABLED = "monitor_enabled"
        const val KEY_LAST_ERROR = "last_error"
        private const val CHANNEL = "call_recorder"
        private const val NOTIFICATION_ID = 7
    }
}
