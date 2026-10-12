package android.os;
import java.util.*;
public class Handler {
    private final List<Runnable> pending=new ArrayList<Runnable>();
    public boolean post(Runnable r){pending.add(r);return true;}
    public boolean postDelayed(Runnable r,long delay){pending.add(r);return true;}
    public void removeCallbacks(Runnable r){while(pending.remove(r)){} }
    public void tick(long now){SystemClock.now=now;List<Runnable> old=new ArrayList<Runnable>(pending);pending.clear();for(Runnable r:old)r.run();}
    public int count(){return pending.size();}
}
