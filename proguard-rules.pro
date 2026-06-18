# ─── GSE Terminal — ProGuard / R8 Rules ────────────────────────────────────────

# Keep all public WebView JavaScript interface methods
-keepclassmembers class com.gseterminal.app.WebAppInterface {
    public *;
}

# Keep Kotlin metadata (required for reflection-based Kotlin features)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# AndroidX WebKit
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# Keep JS-Bridge class names intact (WebView finds them by name)
-keepnames class com.gseterminal.app.** { *; }

# Preserve JavaScript interface annotations
-keepattributes JavascriptInterface

# Prevent stripping of classes used via reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# OkHttp / network (if added later)
-dontwarn okhttp3.**
-dontwarn okio.**

# Suppress harmless warnings
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
