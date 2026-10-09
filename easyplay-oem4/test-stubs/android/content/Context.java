package android.content;
// JVM test fake only; excluded from APK and production compilation.
public abstract class Context {
    public static final int MODE_PRIVATE=0;
    public Context getApplicationContext() { return this; }
    public abstract SharedPreferences getSharedPreferences(String name,int mode);
}
