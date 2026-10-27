بسته اصلاحی ضبط مکالمه

این بسته فقط فایل‌های جایگزین را دارد؛ خودش APK نیست.

فایل‌ها را در ریشه مخزن CallRecorderFA و دقیقاً با همین مسیرها جایگزین کنید:
- app/src/main/AndroidManifest.xml
- app/src/main/java/ir/balasari/callrecorder/MainActivity.kt
- app/src/main/java/ir/balasari/callrecorder/CallStateReceiver.kt
- app/src/main/java/ir/balasari/callrecorder/RecordingService.kt

سپس پروژه را با Gradle بسازید یا GitHub Actions را اجرا کنید.

آزمایش:
1. برنامه را باز کنید و مجوز میکروفون، وضعیت تماس و اعلان‌ها را بدهید.
2. دکمه «فعال‌سازی ضبط خودکار» را بزنید و مطمئن شوید اعلان دائمی دیده می‌شود.
3. یک تماس آزمایشی با رضایت طرف مقابل انجام دهید.
4. پس از پایان تماس، «به‌روزرسانی فهرست فایل‌ها» را بزنید.

محدودیت مهم:
برنامه از AudioSource.MIC استفاده می‌کند. اندروید و سازنده گوشی ممکن است صدای طرف مقابل را در دسترس نگذارند؛ موفقیت ساخت APK به معنی تضمین ضبط دوطرفه نیست. این بسته هنوز روی گوشی واقعی شما آزمایش نشده است.
