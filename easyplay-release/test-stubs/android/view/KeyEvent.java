package android.view;
public class KeyEvent {
 private final long down,time;private final int action,key,repeat,meta,device,scan,flags,source;
 public KeyEvent(long d,long t,int a,int k,int r,int m,int dev,int s,int f,int src){down=d;time=t;action=a;key=k;repeat=r;meta=m;device=dev;scan=s;flags=f;source=src;}
 public long getDownTime(){return down;}public long getEventTime(){return time;}
 public int getAction(){return action;}public int getKeyCode(){return key;}public int getRepeatCount(){return repeat;}
 public int getMetaState(){return meta;}public int getDeviceId(){return device;}public int getScanCode(){return scan;}
 public int getFlags(){return flags;}public int getSource(){return source;}
}
