-dontwarn com.houvven.**

# Loaded by LSPosed from META-INF/xposed/java_init.list. R8 cannot infer this
# resource-to-class reference, so the entry point and its callbacks must retain
# both their binary name and members.
-keep class com.houvven.guise.xposed.HookInit {
    *;
}

-keep class com.houvven.guise.xposed.config.* {
    <fields>;
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
