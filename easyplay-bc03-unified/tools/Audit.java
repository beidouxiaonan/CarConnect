import java.io.*;
import java.util.*;
import java.util.regex.*;
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
    static boolean replacement(String type){return type.equals(P+"Bc03Access;")||type.startsWith(P+"Bc03Access$")||type.equals(P+"OemTransport;")||type.startsWith(P+"OemTransport$");}
    static boolean addition(String type){for(String n:new String[]{"OemSppGate","OemTransport","Bc03Profiles","OemSocketOutput","OemConnectDeadline"})if(type.equals(P+n+";")||type.startsWith(P+n+"$"))return true;return false;}
    static String labels(String s){return s.replace("Carplay-connect-0.1.10-beta-OEM-SPP-guard-test","Carplay-connect-0.1.11-beta-BC03-unified-test").replace("CarConnect 0.1.10 beta","CarConnect 0.1.11 beta (BC03 unified)").replace("BC03 1.7.9 / 1.3.6","BC03 1.7.2 / 1.7.9 / 1.3.6");}
    static final Set<String> AV=new TreeSet<>(),HELPERS=new TreeSet<>();
    static {
        for(String n:new String[]{"media/LegacyAudioRenderer","airplay/AudioStream","legacy/LegacyActivity"})AV.add("Lcom/shilapi/xcertplay/"+n+";");
        for(String n:new String[]{"PcmBufferState","StableAudioTrack","AudioRuntime","FullscreenRecovery","FullscreenRecovery$Controller","AvDiagnostics","AvDiagnostics$1"})HELPERS.add(P+n+";");
    }
    static String diagnosticMethod(String s)throws Exception {
        Matcher m=Pattern.compile("(?ms)^\\.method [^\\n]* showDiagnostics\\(Landroid/app/Activity;\\)V\\n.*?^\\.end method").matcher(s);
        if(!m.find())throw new Exception("Missing diagnostics");String result=m.group();if(m.find())throw new Exception("Duplicate diagnostics");return result;
    }
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> now=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(args[1],Opcodes.forApi(19)).getClasses())now.put(c.getType(),c);
        Map<String,ClassDef> av=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(args[2],Opcodes.forApi(19)).getClasses())av.put(c.getType(),c);
        int preserved=0,changed=0,transport=0,avRetained=0;
        for(ClassDef old:DexFileFactory.loadDexFile(args[0],Opcodes.forApi(19)).getClasses()){
            ClassDef c=now.remove(old.getType());if(c==null)throw new Exception("Removed "+old.getType());
            if(replacement(old.getType())){transport++;continue;}
            String before=canonical(old),after=canonical(c);
            if(AV.contains(old.getType())){
                if(!canonical(av.get(old.getType())).equals(after))throw new Exception("Fixed AV reference changed "+old.getType());avRetained++;continue;
            }
            if(before.equals(after)){preserved++;continue;}
            String expected=labels(before);
            if(old.getType().equals(P+"EnhancementPanel;"))expected=expected.replace(diagnosticMethod(expected),diagnosticMethod(canonical(av.get(old.getType()))));
            if(!expected.equals(after))throw new Exception("Unrelated logic changed "+old.getType());changed++;
        }
        for(String type:HELPERS){ClassDef c=now.remove(type);if(c==null||!canonical(av.get(type)).equals(canonical(c)))throw new Exception("Fixed AV helper changed "+type);avRetained++;}
        for(String t:now.keySet())if(!addition(t))throw new Exception("Unexpected added class "+t);
        if(!now.containsKey(P+"OemSppGate;"))throw new Exception("Missing 1.7.2 gate");
        for(String n:new String[]{"Bc03Profiles","OemSocketOutput","OemConnectDeadline"})if(!now.containsKey(P+n+";"))throw new Exception("Missing unified helper "+n);
        System.out.println("Preserved="+preserved+" labels/AV-diagnostics="+changed+" authored-transport/access="+transport+" fixed-AV="+avRetained+" added="+now.keySet());
        System.out.println("OEM 0.1.10 iAP2/USB/video/touch/phone/first-frame recovery/recovery journal/Service pause/Binder schema/native frames preserved; 1.7.2 audio/fullscreen retained exactly");
    }
}
