# Project-specific R8 rules.
# Compose, Hilt, Room, and AndroidX ship their own consumer rules; only add
# rules here when a keep decision is genuinely required.

# Keep source file names and line numbers so release crash reports stay useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug logging and stray stdout from release builds so internal
# state and identifiers never surface in logcat or a bug-report dump.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void print(...);
    public static void println(...);
}
