# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# Hilt / Dagger generated code
-dontwarn dagger.hilt.**

# Keep data classes used with Gson/Moshi-style reflection in the seed layer
-keep class com.hakunakuinama.app.data.local.entity.** { *; }
