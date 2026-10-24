package ir.balasari.callrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface
import java.io.File

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private val requestCode = 41
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (!hasPermissions()) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE), requestCode)
    }
    private fun hasPermissions() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 24); layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL }
        root.setBackgroundColor(0xFFF5F7FA.toInt())
        val title = TextView(this).apply { text = "ضبط مکالمه"; textSize = 28f; setTextColor(0xFF12304A.toInt()); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 12, 0, 18) }
        root.addView(title)
        status = TextView(this).apply { text = "وضعیت: آماده؛ ضبط خودکار فقط در گوشی‌های سازگار ممکن است."; textSize = 16f; setTextColor(0xFF263746.toInt()); setPadding(14, 16, 14, 16); setBackgroundColor(0xFFE4EDF5.toInt()) }
        root.addView(status)
        val note = TextView(this).apply { text = "توجه: محدودیت‌های اندروید و سازنده گوشی ممکن است صدای طرف مقابل را مسدود کنند. این برنامه تضمین نمی‌کند ضبط دوطرفه روی همه گوشی‌ها کار کند."; textSize = 14f; setTextColor(0xFF8A3B16.toInt()); setPadding(4, 16, 4, 16) }
        root.addView(note)
        val refresh = Button(this).apply { text = "به‌روزرسانی فهرست فایل‌ها"; setOnClickListener { refreshFiles() } }
        root.addView(refresh)
        val shareAll = Button(this).apply { text = "اشتراک‌گذاری آخرین فایل"; setOnClickListener { shareLatest() } }
        root.addView(shareAll)
        val settings = Button(this).apply { text = "تنظیمات دسترسی برنامه"; setOnClickListener { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:$packageName"))) } }
        root.addView(settings)
        val heading = TextView(this).apply { text = "فایل‌های ضبط‌شده"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 18, 0, 8) }
        root.addView(heading)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshFiles()
    }
    private fun refreshFiles() {
        list.removeAllViews()
        val dir = File(getExternalFilesDir(null), "ضبط-مکالمه")
        val files = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) { list.addView(TextView(this).apply { text = "هنوز فایلی ذخیره نشده است."; textSize = 15f; setPadding(0, 8, 0, 8) }); return }
        files.forEach { file ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 6, 0, 6) }
            row.addView(TextView(this).apply { text = file.name; textSize = 13f }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply { text = "اشتراک"; setOnClickListener { share(file) } })
            list.addView(row)
        }
    }
    private fun shareLatest() { val dir = File(getExternalFilesDir(null), "ضبط-مکالمه"); val file = dir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }; if (file == null) Toast.makeText(this, "فایلی برای اشتراک‌گذاری نیست", Toast.LENGTH_SHORT).show() else share(file) }
    private fun share(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply { type = "audio/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فایل ضبط‌شده"))
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); status.text = if (hasPermissions()) "دسترسی پایه داده شد. سازگاری ضبط تماس به گوشی بستگی دارد." else "برای استفاده، دسترسی میکروفون و وضعیت تماس لازم است." }
}
