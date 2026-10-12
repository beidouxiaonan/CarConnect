package android.media;
import android.content.ComponentName;
public class AudioManager {
 public static final int MODE_NORMAL=0;public int mode;
 public int getMode(){return mode;}
 public int receivers,clients;public boolean fail;public RemoteControlClient lastClient;
 public void registerMediaButtonEventReceiver(ComponentName c){if(fail)throw new SecurityException();receivers++;}
 public void registerRemoteControlClient(RemoteControlClient c){clients++;lastClient=c;}
}
