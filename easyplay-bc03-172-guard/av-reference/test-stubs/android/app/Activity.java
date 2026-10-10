package android.app;
import android.view.Window;
public class Activity {
    public final Window window=new Window();
    public boolean focus,finishing;
    public Window getWindow(){return window;}
    public boolean hasWindowFocus(){return focus;}
    public boolean isFinishing(){return finishing;}
}
