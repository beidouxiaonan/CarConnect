import java.util.*;
import java.util.regex.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** All baseline features remain; only bootstrap, labels and the combined log export can differ. */
public class UnifiedAudit {
    static final String P="Lcom/shilapi/xcertplay/patch/";
    static final String OLD="CarConnect-0.1.17-BC03-Unified-FirstFrame30s-AllLogs-Android4.2plus-ARMv7";
    static final String NEW="CarConnect-0.1.18-SD8227-Unified-BC03-FirstFrame30s-AllLogs-Android4.2plus-ARMv7";
    static String labels(String text){return text.replace(OLD,NEW).replace("CarConnect 0.1.17","CarConnect 0.1.18").replace("CarConnect 0.1.16","CarConnect 0.1.18").replace("BC03/30s/AllLogs","SD8227/BC03/30s/AllLogs");}
    static Map<String,ClassDef> combined(String work,String prefix)throws Exception {
        Map<String,ClassDef> result=new TreeMap<>();
        for(String file:new String[]{"classes.dex","classes2.dex","classes3.dex"}) {
            String name=file.equals("classes2.dex")&&prefix.equals("patched-")?"original-"+file:prefix+file;
            for(ClassDef c:UnifiedDex.read(work+"/"+name).values())if(result.put(c.getType(),c)!=null)throw new Exception("Duplicate DEX class "+c.getType());
        }
        return result;
    }
    public static void main(String[] args)throws Exception {
        String w=args[0];Map<String,ClassDef> base=combined(w,"original-"),now=combined(w,"patched-");
        Map<String,ClassDef> authored=UnifiedDex.read(w+"/feature.dex");int same=0,changed=0;
        Set<String> ui=new HashSet<>(Arrays.asList(P+"EnhancementPanel;",P+"CarConnectInfo;",P+"AvDiagnostics;",P+"OemSppPause;",P+"OemTransport;",P+"SppRecoverySettings;","Lcom/shilapi/xcertplay/legacy/LegacyConnectionScreen;","Lcom/shilapi/xcertplay/legacy/LegacySessionService;"));
        for(String type:base.keySet()){
            ClassDef n=now.remove(type);if(n==null)throw new Exception("Baseline feature removed "+type);
            String before=StartupAudit.canonical(base.get(type)),after=StartupAudit.canonical(n);
            if(before.equals(after)){same++;continue;}
            if(type.equals("Landroidx/multidex/MultiDex;")||type.equals("Lcom/shilapi/xcertplay/legacy/LegacyActivity;")){changed++;continue;}
            if(authored.containsKey(type)){
                if(!after.equals(StartupAudit.canonical(authored.get(type))))throw new Exception("Log helper replacement differs "+type);
            }else if(!ui.contains(type)||!labels(before).equals(after))throw new Exception("Unrelated 0.1.17 functionality changed "+type);
            changed++;
        }
        for(String type:now.keySet())if(!type.startsWith("Lio/github/beidouxiaonan/carconnect/startup/"))throw new Exception("Unexpected new helper "+type);
        Map<String,ClassDef> main=UnifiedDex.read(w+"/patched-classes.dex");
        if(!main.containsKey(UnifiedDex.PROVIDER)||UnifiedDex.read(w+"/patched-classes3.dex").containsKey(UnifiedDex.PROVIDER))throw new Exception("Provider must be primary-only");
        for(String type:main.keySet())if(type.equals(UnifiedDex.PROVIDER)||type.startsWith("Lio/github/beidouxiaonan/carconnect/startup/"))
            for(Method method:main.get(type).getMethods())if(method.getImplementation()!=null)
                for(Instruction instruction:method.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction){
                    Reference r=((ReferenceInstruction)instruction).getReference();String owner=null;
                    if(r instanceof MethodReference)owner=((MethodReference)r).getDefiningClass();
                    if(r instanceof FieldReference)owner=((FieldReference)r).getDefiningClass();
                    if(r instanceof TypeReference)owner=((TypeReference)r).getType();
                    if(owner!=null&&!owner.startsWith("Ljava/")&&!owner.startsWith("Landroid/")&&!owner.startsWith("[")&&!main.containsKey(owner))throw new Exception("Secondary dependency in bootstrap "+owner);
                }
        ClassDef frame=combined(w,"patched-").get(P+"FirstFramePolicy;");boolean field=false,deadline=false;
        for(Field f:frame.getFields())if(f.getName().equals("WAIT_MS"))field=((com.android.tools.smali.dexlib2.iface.value.LongEncodedValue)f.getInitialValue()).getValue()==30000L;
        for(Method method:frame.getMethods())if(method.getName().equals("check"))for(Instruction i:method.getImplementation().getInstructions())
            if(i instanceof WideLiteralInstruction&&((WideLiteralInstruction)i).getWideLiteral()==30000L)deadline=true;
        if(!field||!deadline)throw new Exception("Final SD8227 APK first-frame policy is not 30000ms");
        System.out.println("All 0.1.17 baseline classes retained: unchanged="+same+" permitted changes="+changed+" new bootstrap="+now.size());
        System.out.println("Final SD8227 first-frame field and actual deadline both verified 30000ms");
        System.out.println("Primary-only startup/provider dependency audit passed; all AV/wheel/BC03/30s/recovery/USB/iAP2 helpers retained");
    }
}
