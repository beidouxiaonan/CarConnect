package com.shilapi.xcertplay.legacy;
import android.content.*;
import java.util.*;
import com.shilapi.xcertplay.orchestration.CarPlayController;
/** Fake only. Saved preferences are separate from the Service automatic field. */
public class LegacySessionService extends Context {
    public int generation=1,stops;public boolean closing,wired,automatic=true,lastRetain;
    public CarPlayController controller=new CarPlayController();
    public Runnable listener,recordListener;public final List<String> logs=new ArrayList<String>();
    public boolean getClosing(){return closing;}
    public boolean getWaitingAutomatically(){return automatic;}
    public boolean getWired$legacy(){return wired;}
    public CarPlayController getController(){return controller;}
    public void stopSession(boolean keep,boolean retain){
        stops++;lastRetain=retain;if(!retain)automatic=false;
        generation++;closing=true;controller=null;
        if(listener!=null)listener.run();
    }
    public static int access$getGeneration$p(LegacySessionService s){return s.generation;}
    public static void access$record(LegacySessionService s,String text){s.logs.add(text);if(s.recordListener!=null)s.recordListener.run();}
    public static void access$update(LegacySessionService s,String text){s.logs.add(text);if(s.listener!=null)s.listener.run();}
    public SharedPreferences getSharedPreferences(String name,int mode){throw new AssertionError("No preferences changed by retry pause");}
}
