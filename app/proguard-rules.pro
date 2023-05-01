# For stack traces
-keepattributes SourceFile, LineNumberTable

# Get rid of package names, makes file smaller
-repackageclasses

# Keep all PINcredible class names
-keep class com.cyb3rko.pincredible.**

# Explicitely keep Serializable class names
-keepnames class * implements com.cyb3rko.backpack.data.Serializable

# Explicitely keep Serializable class members
-keepclassmembers class * implements com.cyb3rko.backpack.data.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}