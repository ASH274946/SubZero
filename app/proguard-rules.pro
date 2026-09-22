# SubZero Proguard / R8 Rules
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep class com.subzero.data.** { *; }
-keep class com.subzero.engine.** { *; }

# ProfileInstaller
-keep class androidx.profileinstaller.** { *; }

# Room SQLite & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Coroutines Flow reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# MediaPipe compile-time annotation processor references
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**


