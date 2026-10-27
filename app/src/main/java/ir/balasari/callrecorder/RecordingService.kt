package ir.balasari.callrecorder

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ذخیره وضعیت/تنظیمات تا هم سرویس و هم صفحه اصلی بتوانند بخوانند. */
object Prefs {
    val SOURCES = listOf(
        "MIC" to MediaRecorder.AudioSource.MIC,
        "VOICE_RECOGNITION" to MediaRecorder.AudioSource.VOICE_RECOGNITION,
        "VOICE_COMMUNICATION" to MediaRecorder.AudioSource.VOICE_COMMUNICATION
    )
    private fun p(c: Context) = c.getSharedPreferences("state", Context.MODE_PRIVATE)
    fun isEnabled(c: Context) = p(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()
    fun status(c: Context) = p(c).getString("status", "آماده") ?: "آماده"
    fun setStatus(c: Context, s: String) = p(c).edit().putString("status", s).apply()
    fun sourceIndex(c: Context) = p(c).getInt("source", 0).coerceIn(0, SOURCES.size - 1)
    fun setSourceIndex(c: Context, i: Int) = p(c).edit().putInt("source", i).apply()
}

class RecordingService : Service() {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var telephony: TelephonyManager? = null
    private var callback: Any? = null   // TelephonyCallback روی اندروید ۱۲+
    private var legacy: Any? = null     // PhoneStateListener روی نسخه‌های قدیمی‌تر

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISABLE) { disable(); return START_NOT_STICKY }
        enable()
        return START_STICKY
    }

    private fun enable() {
        createChannel()
        try {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIF_ID, n)
        } catch (e: Exception) {
            fail("شروع سرویس ناموفق بود: ${e.javaClass.simpleName}: ${e.message}. برنامه را باز کنید و دوباره فعال کنید.")
            return
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("دسترسی میکروفون و وضعیت تلفن داده نشده است.")
            return
        }
        if (callback == null && legacy == null) {
            try { registerListener() } catch (e: Exception) { fail("ثبت تشخیص تماس ناموفق بود: ${e.message}"); return }
        }
        Prefs.setEnabled(this, true)
        if (recorder == null) Prefs.setStatus(this, "ضبط خودکار فعال است؛ منتظر تماس")
    }

    private fun fail(msg: String) {
        Prefs.setEnabled(this, false)
        Prefs.setStatus(this, msg)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun disable() {
        stopRecording()
        unregisterListener()
        Prefs.setEnabled(this, false)
        Prefs.setStatus(this, "ضبط خودکار غیرفعال است")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun registerListener() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephony = tm
        if (Build.VERSION.SDK_INT >= 31) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = handleState(state)
            }
            callback = cb
            tm.registerTelephonyCallback(mainExecutor, cb)
        } else {
            val l = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) = handleState(state)
            }
            legacy = l
            tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterListener() {
        try {
            if (Build.VERSION.SDK_INT >= 31) (callback as? TelephonyCallback)?.let { telephony?.unregisterTelephonyCallback(it) }
            else (legacy as? PhoneStateListener)?.let { telephony?.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (_: Exception) {}
        callback = null; legacy = null
    }

    private fun handleState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> startRecording()
            TelephonyManager.CALL_STATE_IDLE -> stopRecording()
        }
    }

    private fun startRecording() {
        if (recorder != null) return
        var r: MediaRecorder? = null
        try {
            val dir = File(getExternalFilesDir(null), "ضبط-مکالمه").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "Call_$stamp.m4a")
            output = file
            val (srcName, src) = Prefs.SOURCES[Prefs.sourceIndex(this)]
            r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(src)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare(); r.start()
            recorder = r
            Prefs.setStatus(this, "در حال ضبط ($srcName) ← ${file.name}")
        } catch (e: Exception) {
            try { r?.release() } catch (_: Exception) {}
            recorder = null
            output?.delete()
            Prefs.setStatus(this, "شروع ضبط ناموفق بود: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun stopRecording() {
        val r = recorder ?: return
        recorder = null
        val file = output
        try {
            r.stop()
            val kb = (file?.length() ?: 0) / 1024
            Prefs.setStatus(this, "ذخیره شد: ${file?.name} (${kb} کیلوبایت). اگر بی‌صدا بود، منبع صدا را عوض کنید یا بلندگو را روشن کنید.")
        } catch (e: Exception) {
            file?.delete()
            Prefs.setStatus(this, "ضبط خیلی کوتاه بود یا ناموفق شد و حذف شد")
        }
        try { r.release() } catch (_: Exception) {}
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("ضبط مکالمه فعال است")
            .setContentText("منتظر تماس؛ برای باز کردن برنامه لمس کنید")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "وضعیت ضبط مکالمه", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onDestroy() {
        stopRecording()
        unregisterListener()
        super.onDestroy()
    }

    companion object {
        const val ACTION_ENABLE = "ir.balasari.callrecorder.ENABLE"
        const val ACTION_DISABLE = "ir.balasari.callrecorder.DISABLE"
        private const val CHANNEL = "call_recorder"
        private const val NOTIF_ID = 7
    }
}
