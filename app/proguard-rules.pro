# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities, DAOs, Databases & TypeConverters
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.system.AppDatabase { *; }
-keep class com.example.system.DatabaseConverters { *; }

# Keep Android Services, Receivers, Providers & Activities declared in AndroidManifest
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class * extends android.service.notification.NotificationListenerService { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.core.content.FileProvider { *; }

# Media3 & ML Kit Text Recognition
-keep class androidx.media3.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Kyant Backdrop & Haze glass shaders (keep runtime shader hooks)
-keep class com.kyant.backdrop.** { *; }
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn com.kyant.backdrop.**
-dontwarn dev.chrisbanes.haze.**

# Optional MIUI / OEM reflection helpers
-dontwarn android.os.SystemProperties
-dontwarn android.hardware.camera2.**
