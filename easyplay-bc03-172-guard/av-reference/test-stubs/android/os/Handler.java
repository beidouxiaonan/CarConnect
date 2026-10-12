package android.os;
import java.util.*;
public class Handler {
    private static final List<Entry> tasks=new ArrayList<Entry>();
    private static class Entry {Runnable r;long at;Entry(Runnable r,long at){this.r=r;this.at=at;}}
    public boolean postDelayed(Runnable r,long delay){tasks.add(new Entry(r,SystemClock.now+delay));return true;}
    public void removeCallbacks(Runnable r){for(Iterator<Entry> i=tasks.iterator();i.hasNext();)if(i.next().r==r)i.remove();}
    public static void reset(){tasks.clear();SystemClock.now=0;}
    public static int pending(){return tasks.size();}
    public static void advance(long millis){
        long end=SystemClock.now+millis;
        for(int guard=0;guard<30;guard++){
            Entry next=null;for(Entry e:tasks)if(e.at<=end&&(next==null||e.at<next.at))next=e;
            if(next==null){SystemClock.now=end;return;}
            tasks.remove(next);SystemClock.now=next.at;next.r.run();
        }
        throw new AssertionError("Unbounded retry loop");
    }
}
