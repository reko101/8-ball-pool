# Pool Security Lab - R8 / ProGuard configuration
#
# The application has no reflection-based entry points other than the ones the
# Android framework itself resolves, so the default optimised rules are enough.
# The rules below only keep what the framework instantiates by name.

-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep the line numbers so that a stack trace collected during the laboratory
# session can still be mapped back to the source with the generated mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Educational note:
# Shrinking and obfuscation raise the cost of static reverse engineering, but
# they are NOT a security control on their own. Every value that the client
# owns can still be located at runtime. See the in-app Security Report screen.
