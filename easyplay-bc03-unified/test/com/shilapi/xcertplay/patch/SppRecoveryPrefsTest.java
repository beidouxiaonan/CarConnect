package com.shilapi.xcertplay.patch;

import android.content.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SppRecoveryPrefsTest {
    private static final class Prefs implements SharedPreferences {
        boolean commitOk=true;
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
            public boolean commit(){return commitOk;}
            public void apply(){}
        };}
    }
    private static Context context(final Prefs p){return new Context(){public SharedPreferences getSharedPreferences(String n,int m){assertEquals("easyplay_enhancements",n);return p;}};}
    @Test public void nativeDefaultsOffEvenWhenOldCleanupWasEnabled(){Prefs p=new Prefs();p.values.put("oemSppCleanup",true);assertFalse(SppRecoveryPrefs.nativeEnabled(context(p)));assertTrue(SppCleanupPrefs.enabled(context(p)));}
    @Test public void nativeChoicePersistsAcrossNewContext(){Prefs p=new Prefs();assertTrue(SppRecoveryPrefs.saveNative(context(p),true));assertTrue(SppRecoveryPrefs.nativeEnabled(context(p)));assertTrue(SppRecoveryPrefs.saveNative(context(p),false));assertFalse(SppRecoveryPrefs.nativeEnabled(context(p)));}
    @Test public void pendingPersistsAcrossNewContext(){Prefs p=new Prefs();assertFalse(SppRecoveryPrefs.pending(context(p)));assertTrue(SppRecoveryPrefs.begin(context(p)));assertTrue(SppRecoveryPrefs.pending(context(p)));}
    @Test public void clearingPendingDoesNotDisableNative(){Prefs p=new Prefs();SppRecoveryPrefs.saveNative(context(p),true);SppRecoveryPrefs.begin(context(p));assertTrue(SppRecoveryPrefs.released(context(p)));assertFalse(SppRecoveryPrefs.pending(context(p)));assertTrue(SppRecoveryPrefs.nativeEnabled(context(p)));}
    @Test public void newKeysDoNotChangePhoneOrAutomaticOrLevels(){Prefs p=new Prefs();p.values.put("phone","saved");p.values.put("automatic",true);p.values.put("navigationVolume",45);p.values.put("oemSppCleanup",true);SppRecoveryPrefs.begin(context(p));SppRecoveryPrefs.saveNative(context(p),true);SppRecoveryPrefs.released(context(p));assertEquals("saved",p.getString("phone",null));assertTrue(p.getBoolean("automatic",false));assertEquals(45,p.getInt("navigationVolume",0));assertTrue(p.getBoolean("oemSppCleanup",false));}
    @Test public void failedBeginIsReported(){Prefs p=new Prefs();p.commitOk=false;assertFalse(SppRecoveryPrefs.begin(context(p)));}
    @Test public void failedClearIsReported(){Prefs p=new Prefs();SppRecoveryPrefs.begin(context(p));p.commitOk=false;assertFalse(SppRecoveryPrefs.released(context(p)));}
    @Test public void failedNativeSaveIsReported(){Prefs p=new Prefs();p.commitOk=false;assertFalse(SppRecoveryPrefs.saveNative(context(p),true));}
}
