package com.shilapi.xcertplay.patch;
/** Compile-only signatures; real existing trace journal stays in the APK. */
public final class OemTrace {
 public static void event(String value){throw new AssertionError();}
 public static void error(String value,Throwable error){throw new AssertionError();}
 public static String summary(android.content.Context c){throw new AssertionError();}
}
