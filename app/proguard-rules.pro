# R8 压缩开启后的保留规则
# 农历库内部有静态表与反射调用，整体保留（体积约 1MB，可接受）
-keep class com.nlf.calendar.** { *; }
# 崩溃堆栈保留源码行号，便于排查
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
