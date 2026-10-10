import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
public class Audit {
    static final String P="Lcom/shilapi/xcertplay/patch/", S="Lcom/shilapi/xcertplay/legacy/LegacySessionService;";
    static String canonical(ClassDef c)throws Exception {
        BaksmaliOptions o=new BaksmaliOptions();o.apiLevel=19;o.sequentialLabels=true;
        StringWriter s=new StringWriter();BaksmaliWriter w=new BaksmaliWriter(s);
        ClassDefinition d=new ClassDefinition(o,c);d.writeTo(w);w.flush();
        if(d.hadValidationErrors())throw new Exception("Invalid class "+c.getType());
        return s.toString().replace("\r\n","\n").replace("const-string/jumbo ","const-string ")
            .replaceAll("(?m)^    nop\\n\\n(?=    :(?:pswitch|sswitch)_data_)","")
            .replaceAll("(?m)^(\\.field [^\\r\\n]+?) = (false|0x0|null|0\\.0f?)(?=\\r?$)","$1");
    }
    static boolean replacement(String type){return type.equals(P+"SppRelease;")||type.startsWith(P+"SppRelease$")||type.equals(P+"OemTransport;")||type.startsWith(P+"OemTransport$");}
    static boolean addition(String type){for(String n:new String[]{"SppRelease","SppRecoveryPrefs","SppRecoverySettings","OemSppPause","OemTransport"})if(type.equals(P+n+";")||type.startsWith(P+n+"$"))return true;return false;}
    static String labels(String s){return s.replace("Carplay-connect-0.1.9-beta-OEM-first-frame-diagnostics-test","Carplay-connect-0.1.10-beta-OEM-SPP-guard-test").replace("CarConnect 0.1.9 beta","CarConnect 0.1.10 beta");}
    static String restore(String s){return s.replace(P+"SppRecoverySettings;->add",P+"SppCleanupSettings;->add").replace("invoke-static {v3}, "+P+"OemSppPause;->failed("+S+")V","invoke-virtual {v3, p0, p0}, "+S+"->stopSession(ZZ)V");}
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> now=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(args[1],Opcodes.forApi(19)).getClasses())now.put(c.getType(),c);
        int preserved=0,changed=0,transport=0;
        for(ClassDef old:DexFileFactory.loadDexFile(args[0],Opcodes.forApi(19)).getClasses()){
            ClassDef c=now.remove(old.getType());if(c==null)throw new Exception("Removed "+old.getType());
            if(replacement(old.getType())){transport++;continue;}
            String before=canonical(old),after=canonical(c);
            if(before.equals(after)){preserved++;continue;}
            if(!labels(before).equals(restore(after)))throw new Exception("Unrelated logic changed "+old.getType());changed++;
        }
        for(String t:now.keySet())if(!addition(t))throw new Exception("Unexpected added class "+t);
        for(String n:new String[]{"SppRecoveryPrefs","SppRecoverySettings","OemSppPause"})if(!now.containsKey(P+n+";"))throw new Exception("Missing "+n);
        System.out.println("Preserved="+preserved+" labels/two-call-sites="+changed+" authored-transport="+transport+" added="+now.keySet());
        System.out.println("Original iAP2/USB/video/audio/touch/phone/first-frame recovery/Binder schema/native frames preserved");
    }
}
