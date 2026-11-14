# Project-specific R8 rules.
# Compose, Hilt, Room, and AndroidX ship their own consumer rules; only add
# rules here when a keep decision is genuinely required.

# Keep source file names and line numbers so release crash reports stay useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
