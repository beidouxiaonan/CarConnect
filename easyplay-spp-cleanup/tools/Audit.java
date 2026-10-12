import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;

public class Audit {
    static final String P="Lcom/shilapi/xcertplay/patch/";
    static String canonical(ClassDef c)throws Exception {
        BaksmaliOptions o=new BaksmaliOptions();o.apiLevel=19;o.sequentialLabels=true;
        StringWriter s=new StringWriter();BaksmaliWriter w=new BaksmaliWriter(s);
        ClassDefinition d=new ClassDefinition(o,c);d.writeTo(w);w.flush();
        if(d.hadValidationErrors())throw new Exception("Invalid class "+c.getType());
        return s.toString().replace("\r\n","\n").replace("const-string/jumbo ","const-string ")
            .replaceAll("(?m)^    nop\\n\\n(?=    :(?:pswitch|sswitch)_data_)","")
            .replaceAll("(?m)^(\\.field [^\\r\\n]+?) = (false|0x0|null|0\\.0f?)(?=\\r?$)","$1");
    }
    static String display(String s) {
        return s.replace("Carplay-connect-0.1.4-beta-OEM-test","Carplay-connect-0.1.8-beta-OEM-SPP-native-recovery-test")
            .replace("CarConnect 0.1.4 beta","CarConnect 0.1.8 beta");
    }
    static boolean transport(String t){
        for(String n:new String[]{"OemTransport","Bc03Access","Bc03Schema","SppNativeCommand","SppVfWindow","SppRelease","SppCleanupPrefs","SppCleanupSettings"})
            if(t.equals(P+n+";")||t.startsWith(P+n+"$"))return true;
        return false;
    }
    static String stripHooks(String s){
        return s.replace("    invoke-static {p0}, "+P+"OemTransport;->startup(Landroid/content/Context;)V\n\n","")
          .replace("    invoke-static {p0, v0}, "+P+"SppCleanupSettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V\n\n","");
    }
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> now=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(args[1],Opcodes.forApi(19)).getClasses())now.put(c.getType(),c);
        int preserved=0,labels=0,transport=0;
        for(ClassDef old:DexFileFactory.loadDexFile(args[0],Opcodes.forApi(19)).getClasses()) {
            ClassDef c=now.remove(old.getType());if(c==null)throw new Exception("Removed "+old.getType());
            String before=canonical(old),after=canonical(c);
            if(transport(old.getType())){transport++;continue;}
            if(before.equals(after)){preserved++;continue;}
            if(old.getType().equals(P+"EnhancementPanel;"))after=stripHooks(after);
            if(!display(before).equals(after))throw new Exception("Non-cleanup logic changed "+old.getType());
            labels++;
        }
        Set<String> added=new TreeSet<>(now.keySet());
        for(String t:added)if(!transport(t))throw new Exception("Unexpected added class "+t);
        for(String n:new String[]{"SppRelease","SppCleanupPrefs","SppCleanupSettings"})if(!added.contains(P+n+";"))throw new Exception("Missing cleanup class "+n);
        System.out.println("Preserved="+preserved+" labels-or-two-UI-hooks="+labels+" authored-transport="+transport+" added="+added);
        System.out.println("Only authored cleanup helpers, two setup hooks and display versions changed; original iAP2/audio/video/touch/phone memory/USB retained");
    }
}
