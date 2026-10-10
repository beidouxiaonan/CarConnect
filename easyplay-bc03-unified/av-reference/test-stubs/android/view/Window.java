package android.view;
public class Window {public final View decor=new View();public int flags;public View getDecorView(){return decor;}public void addFlags(int f){flags|=f;}}
