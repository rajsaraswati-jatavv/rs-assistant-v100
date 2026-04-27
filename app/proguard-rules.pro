# Add project specific ProGuard rules here.
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep class com.rsassistant.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
