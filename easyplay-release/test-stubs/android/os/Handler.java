package android.os;
import java.util.*;
public class Handler {
 private static final List<Task> queue=new ArrayList<Task>();
 private static class Task {Runnable r;long at;Task(Runnable r,long at){this.r=r;this.at=at;} }
 public Handler(Looper l){}
 public boolean postDelayed(Runnable r,long ms){queue.add(new Task(r,SystemClock.now+ms));return true;}
 public void removeCallbacks(Runnable r){for(Iterator<Task> i=queue.iterator();i.hasNext();)if(i.next().r==r)i.remove();}
 public static void reset(){queue.clear();SystemClock.now=0;}
 public static int pending(){return queue.size();}
 public static void advance(long ms){long until=SystemClock.now+ms;int guard=0;while(true){Task next=null;for(Task t:queue)if(t.at<=until&&(next==null||t.at<next.at))next=t;if(next==null)break;if(++guard>1000)throw new AssertionError("timer loop");queue.remove(next);SystemClock.now=next.at;next.r.run();}SystemClock.now=until;}
}
