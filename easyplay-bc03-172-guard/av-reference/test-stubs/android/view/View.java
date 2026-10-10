package android.view;
public class View {
    public interface OnSystemUiVisibilityChangeListener {void onSystemUiVisibilityChange(int visibility);}
    private int flags;
    private OnSystemUiVisibilityChangeListener listener;
    public boolean reject;
    public int setCalls;
    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l){listener=l;}
    public int getSystemUiVisibility(){return flags;}
    public void setSystemUiVisibility(int f){setCalls++;flags=reject?0:f;if(listener!=null)listener.onSystemUiVisibilityChange(flags);}
    public void systemRestoresBars(){flags=0;if(listener!=null)listener.onSystemUiVisibilityChange(0);}
}
