import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
/** Import only the fixed 1.7.2 release's AV classes and diagnostics method, keeping new connection hooks. */
public class AvOverlay {
    static final String ROOT="Lcom/shilapi/xcertplay/", P=ROOT+"patch/";
    static final String PANEL=P+"EnhancementPanel;";
    static final String[] TYPES={ROOT+"media/LegacyAudioRenderer;",ROOT+"airplay/AudioStream;",ROOT+"legacy/LegacyActivity;",
        P+"PcmBufferState;",P+"StableAudioTrack;",P+"AudioRuntime;",P+"FullscreenRecovery;",P+"FullscreenRecovery$Controller;",P+"AvDiagnostics;",P+"AvDiagnostics$1;"};
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> r=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(path,Opcodes.forApi(19)).getClasses())r.put(c.getType(),c);return r;
    }
    static Method diagnostics(ClassDef c)throws Exception {
        Method found=null;
        for(Method m:c.getMethods())if(m.getName().equals("showDiagnostics")&&m.getParameterTypes().equals(Arrays.asList("Landroid/app/Activity;"))&&m.getReturnType().equals("V")){
            if(found!=null)throw new Exception("Duplicate diagnostics method");found=m;
        }
        if(found==null)throw new Exception("Missing diagnostics method");return found;
    }
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> now=read(args[0]),old=read(args[1]);
        for(String type:TYPES){ClassDef c=old.get(type);if(c==null)throw new Exception("Missing fixed AV reference "+type);now.put(type,c);}
        ClassDef panel=now.get(PANEL);Method target=diagnostics(panel),replacement=diagnostics(old.get(PANEL));
        List<Method> methods=new ArrayList<>();for(Method m:panel.getMethods())methods.add(m.equals(target)?replacement:m);
        now.put(PANEL,new ImmutableClassDef(panel.getType(),panel.getAccessFlags(),panel.getSuperclass(),panel.getInterfaces(),panel.getSourceFile(),panel.getAnnotations(),panel.getFields(),methods));
        DexFileFactory.writeDexFile(args[2],new ImmutableDexFile(Opcodes.forApi(19),now.values()));
        System.out.println("Retained fixed 1.7.2 AV classes="+TYPES.length+" and one diagnostics method; new connection/first-frame hooks retained");
    }
}
