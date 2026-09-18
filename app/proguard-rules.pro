# ──────────────────────────────────────────────
# UrWallet ProGuard / R8 Rules
# ──────────────────────────────────────────────

# ── General ──────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ───────────────────────────────────
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ── Kotlin Coroutines ────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Room ─────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class **_Impl {
    <init>(...);
    *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase$Callback
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    *;
}
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
    *;
}
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }

# ── App Model & Entity Classes ───────────────
-keep class com.example.urwallet.core.database.entity.** { *; }
-keep class com.example.urwallet.core.database.model.** { *; }
-keep class com.example.urwallet.core.database.dao.** { *; }
-keep class com.example.urwallet.features.**.domain.model.** { *; }
-keep class com.example.urwallet.features.**.data.model.** { *; }
-keep class com.example.urwallet.features.**.data.entity.** { *; }

# ── Glide ────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# ── MPAndroidChart ───────────────────────────
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── AndroidX & Material ─────────────────────
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ── AndroidX Security Crypto ─────────────────
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ── AndroidX Biometric ───────────────────────
-keep class androidx.biometric.** { *; }

# ── AndroidX DataStore ───────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── AndroidX WorkManager ─────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Navigation Component ─────────────────────
-keep class * extends androidx.navigation.Navigator

# ── ViewBinding ──────────────────────────────
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# ── Enums ────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}

# ── Parcelable ───────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Serializable ─────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
