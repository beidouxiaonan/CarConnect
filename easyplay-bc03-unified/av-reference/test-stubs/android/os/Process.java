package android.os;
public class Process {
    public static int priority;
    public static boolean deny;
    public static void setThreadPriority(int p){if(deny)throw new SecurityException();priority=p;}
    public static int myTid(){return 1;}
    public static int getThreadPriority(int id){return priority;}
}
