# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# SLF4J warnings
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep all Entity classes
-keep class com.afrinuts.farmos.data.local.entity.** { *; }

# Keep all Dao classes
-keep class * extends androidx.room.Dao

# Keep all Database classes
-keep class com.afrinuts.farmos.data.local.database.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-keep class com.google.gson.** { *; }
-keep class com.afrinuts.farmos.data.remote.** { *; }

# Keep model classes for Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# iText
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**