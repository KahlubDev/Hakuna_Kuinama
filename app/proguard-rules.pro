# ---------------------------------------------------------------------------------
# R8 / ProGuard rules
#
# The default here is *no rules*. Both Room and Hilt/Dagger generate code that
# references your classes directly, so R8 can see every reference and needs no help
# keeping them. The stock `-keep class * extends androidx.room.RoomDatabase` and
# `-keep class **$*_Impl` rules that get copy-pasted into Android templates are
# cargo cult: the first keeps every Room database in every library on the classpath,
# the second keeps generated plumbing that R8 handles correctly on its own.
#
# So only three things below are genuinely required. Everything else is either a
# readability aid (line numbers) or a warning suppression for optional APIs.
# ---------------------------------------------------------------------------------

# ---- 1. Enum names are persisted data, not code ---------------------------------
# TypeConverters stores enums by name: MealSlot.BREAKFAST is written to SQLite as the
# string "BREAKFAST" and read back with MealSlot.valueOf("BREAKFAST").
#
# R8 renames enum constants like any other field. Without this rule, a release build
# silently fails to read every slot, category and difficulty it ever wrote: the
# development build is fine, the installed update throws IllegalArgumentException on
# the first query, and no stack trace points at obfuscation.
-keepclassmembers enum com.hakunakuinama.app.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** <fields>;
}

# ---- 2. Readable crash reports --------------------------------------------------
# Default ProGuard behaviour strips source file and line numbers, so a Play Console
# stack trace is a wall of obfuscated names. This costs nothing and is the single
# highest-value rule in the file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- 3. Optional APIs, referenced but unused ------------------------------------
# java.time is available from minSdk 26, so no desugaring is needed and no
# -dontwarn is required for it. Left here only to document that the decision is
# deliberate rather than forgotten.
-dontwarn javax.annotation.**
