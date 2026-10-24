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
        startForeground(7, Notification.Builder(this, CHANNEL).setContentTitle("ضبط مکالمه").setContentText("وضعیت ضبط تماس بررسی می‌شود").setSmallIcon(android.R.drawable.ic_btn_speak_now).build())
        when (intent?.action) { ACTION_START -> startRecording(); ACTION_STOP -> stopRecording() }
        return START_NOT_STICKY
    }
    private fun startRecording() {
        if (recorder != null) return
        try {
            val dir = File(getExternalFilesDir(null), "ضبط-مکالمه").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            output = File(dir, "Call_$stamp.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(output!!.absolutePath)
            r.prepare(); r.start(); recorder = r
        } catch (_: Exception) { stopRecording() }
    }
    private fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) { output?.delete() }
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) { val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel(CHANNEL, "وضعیت ضبط مکالمه", NotificationManager.IMPORTANCE_LOW)) } }
    override fun onDestroy() { if (recorder != null) stopRecording(); super.onDestroy() }
    companion object { const val ACTION_START = "ir.balasari.callrecorder.START"; const val ACTION_STOP = "ir.balasari.callrecorder.STOP"; private const val CHANNEL = "call_recorder" }
}
