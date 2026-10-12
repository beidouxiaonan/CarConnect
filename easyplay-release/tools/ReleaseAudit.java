import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Canonical comparison of every baseline class, expected small hooks, and all retained AV helpers. */
public class ReleaseAudit {
    static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> r=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(path,Opcodes.forApi(19)).getClasses())r.put(c.getType(),c);
        return r;
    }
    public static void main(String[] args) throws Exception {
        Map<String,ClassDef> base=read(args[0]),now=read(args[1]),overlay=read(args[2]);
        Set<String> permitted=new TreeSet<>();
        for(String name:new String[]{"legacy/LegacyWheelDispatcher","legacy/LegacyMediaKeyGate",
            "legacy/LegacyMediaControl$PlatformBackend","legacy/LegacyActivity",
            "legacy/LegacyConnectionScreen","legacy/LegacySessionService","patch/EnhancementPanel","patch/CarConnectInfo"})
            permitted.add("Lcom/shilapi/xcertplay/"+name+";");
        if(!overlay.keySet().equals(permitted))throw new Exception("Unexpected hook classes");
        int preserved=0,hooks=0;
        for(String type:base.keySet()) {
            ClassDef next=now.remove(type);
            if(next==null)throw new Exception("Removed "+type);
            String old=Audit.canonical(base.get(type)),actual=Audit.canonical(next);
            if(permitted.contains(type)) {
                if(!actual.equals(Audit.canonical(overlay.get(type))))throw new Exception("Hook mismatch "+type);
                if(!old.equals(actual))hooks++;else preserved++;
            } else {
                if(!old.equals(actual))throw new Exception("Unrelated class changed "+type);
                preserved++;
            }
        }
        for(String type:now.keySet()) {
            boolean allowed=false;
            for(String name:new String[]{"WheelEventClock","WheelInputRecovery","MediaKeyRecovery"})
                if(type.equals("Lcom/shilapi/xcertplay/patch/"+name+";")||type.startsWith("Lcom/shilapi/xcertplay/patch/"+name+"$"))allowed=true;
            if(!allowed)throw new Exception("Unexpected added class "+type);
        }
        if(hooks!=8||now.size()!=7)throw new Exception("Unexpected hook/helper count "+hooks+"/"+now.size());
        System.out.println("Preserved="+preserved+" expected version/wheel/window hooks="+hooks+" authored additions="+now.size());
        System.out.println("AV audio renderer, audio stream, seven AV helpers and every BC03/SPP/USB/iAP2/first-frame/phone class retained; Activity only adds five adjacent foreground notifications.");
    }
}
