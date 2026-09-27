# MoneyFamily custom R8 rules.
# AndroidX, Room, Kotlin serialization, Supabase/Ktor and Play Billing
# publish their own consumer rules.

# Keep the Android entry point.
-keep class com.moneyfamily.app.MainActivity { *; }

# Preserve metadata used by Kotlin serialization.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
