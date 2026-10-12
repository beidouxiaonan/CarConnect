package com.shilapi.xcertplay.legacy;
import android.content.*;
import android.os.*;
import android.view.Surface;
import java.util.*;
import com.shilapi.xcertplay.orchestration.CarPlayController;
/** JVM fake only, excluded from APK. Models synchronous listener cancellation and worker close. */
public class LegacySessionService extends Context {
    public int generation=1, stops;public boolean destroyed,closing,automatic=true,wired,frame;
    public final Handler handler=new Handler();public Surface surface=new Surface();
    public CarPlayController controller=new CarPlayController();public final List<String> logs=new ArrayList<String>();
    public Runnable listener;
    public boolean getClosing(){return closing;}
    public boolean getWaitingAutomatically(){return automatic;}
    public boolean getWired$legacy(){return wired;}
    public CarPlayController getController(){return controller;}
    public void stopSession(boolean keepStatus,boolean retain){stops++;FirstFrameHook.stop(this,retain);generation++;closing=true;controller=null;frame=false;}
    public static boolean access$getDestroyed$p(LegacySessionService s){return s.destroyed;}
    public static int access$getGeneration$p(LegacySessionService s){return s.generation;}
    public static Handler access$getMain$p(LegacySessionService s){return s.handler;}
    public static Surface access$getSurface$p(LegacySessionService s){return s.surface;}
    public static boolean access$getVideoFrameSeen$p(LegacySessionService s){return s.frame;}
    public static void access$record(LegacySessionService s,String v){s.logs.add(v);}
    public static void access$update(LegacySessionService s,String v){s.logs.add(v);if(s.listener!=null)s.listener.run();}
    public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
    private final SharedPreferences prefs=new SharedPreferences(){
        boolean enabled=true;
        public Map<String,?> getAll(){return Collections.emptyMap();}
        public String getString(String k,String d){return d;}
        public Set<String> getStringSet(String k,Set<String>d){return d;}
        public int getInt(String k,int d){return d;}public long getLong(String k,long d){return d;}
        public float getFloat(String k,float d){return d;}public boolean getBoolean(String k,boolean d){return enabled;}
        public boolean contains(String k){return true;}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public Editor edit(){return new Editor(){
            public Editor putBoolean(String k,boolean v){enabled=v;return this;}
            public Editor putString(String k,String v){return this;}public Editor putStringSet(String k,Set<String>v){return this;}
            public Editor putInt(String k,int v){return this;}public Editor putLong(String k,long v){return this;}
            public Editor putFloat(String k,float v){return this;}public Editor remove(String k){return this;}
            public Editor clear(){return this;}public boolean commit(){return true;}public void apply(){}
        };}
    };
    static final class FirstFrameHook {
        static void stop(LegacySessionService s,boolean retain){com.shilapi.xcertplay.patch.FirstFrameRecovery.stopped(s,retain);}
    }
}
