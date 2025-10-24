# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK.

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep Room entities
-keep class com.attendance.facerecognition.data.local.entities.** { *; }

# Keep Retrofit models
-keep class com.attendance.facerecognition.data.models.** { *; }
