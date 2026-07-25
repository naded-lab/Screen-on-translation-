# قواعد ProGuard الخاصة بتطبيق ترجمان

# ML Kit Translate يحتاج الاحتفاظ بأصناف النماذج الداخلية حتى تعمل بعد التصغير
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin coroutines / metadata
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers class kotlin.Metadata { *; }

# DataStore protobuf generated classes
-keep class androidx.datastore.*.** { *; }

# لا تحذف الأسماء المستخدمة في serialization اليدوي للـ History
-keepclassmembers class com.tarjaman.offline.data.history.HistoryItem { *; }
