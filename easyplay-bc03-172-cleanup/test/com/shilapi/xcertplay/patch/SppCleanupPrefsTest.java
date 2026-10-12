package com.shilapi.xcertplay.patch;

import android.content.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SppCleanupPrefsTest {
    private static final class Prefs implements SharedPreferences {
        final Map<String,Object> values=new HashMap<String,Object>();
        public Map<String,?> getAll(){return new HashMap<String,Object>(values);}
        public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}
        public Set<String> getStringSet(String k,Set<String>d){return d;}
        public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}
        public long getLong(String k,long d){return d;}
        public float getFloat(String k,float d){return d;}
        public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}
        public boolean contains(String k){return values.containsKey(k);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public Editor edit(){return new Editor(){
            public Editor putString(String k,String v){values.put(k,v);return this;}
            public Editor putStringSet(String k,Set<String>v){return this;}
            public Editor putInt(String k,int v){values.put(k,v);return this;}
            public Editor putLong(String k,long v){return this;}
            public Editor putFloat(String k,float v){return this;}
            public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}
            public Editor remove(String k){values.remove(k);return this;}
            public Editor clear(){values.clear();return this;}
            public boolean commit(){return true;}
            public void apply(){}
        };}
    }
    @Test public void newSwitchDefaultsOffAndKeepsOtherSavedValues() {
        final Prefs prefs=new Prefs();prefs.values.put("navigationVolume",45);prefs.values.put("oemBluetoothTest",true);
        Context context=new Context(){public SharedPreferences getSharedPreferences(String n,int m){assertEquals("easyplay_enhancements",n);return prefs;}};
        assertFalse(SppCleanupPrefs.enabled(context));SppCleanupPrefs.save(context,true);assertTrue(SppCleanupPrefs.enabled(context));
        Context restarted=new Context(){public SharedPreferences getSharedPreferences(String n,int m){return prefs;}};
        assertTrue(SppCleanupPrefs.enabled(restarted));assertEquals(45,prefs.getInt("navigationVolume",0));assertTrue(prefs.getBoolean("oemBluetoothTest",false));
        SppCleanupPrefs.save(restarted,false);assertFalse(SppCleanupPrefs.enabled(context));
    }
}
