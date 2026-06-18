# Keep kotlinx.serialization generated serializers
-keepclassmembers,allowshrinking,allowobfuscation @kotlinx.serialization.Serializable class ** { *; }
-keep,includedescriptorclasses class com.nimboweather.forecast.**$$serializer { *; }
