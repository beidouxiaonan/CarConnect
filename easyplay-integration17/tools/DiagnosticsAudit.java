import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
public class DiagnosticsAudit {
 static Map<String,ClassDef> read(String path)throws Exception {
  Map<String,ClassDef> result=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(path,Opcodes.forApi(19)).getClasses())result.put(c.getType(),c);return result;
 }
 public static void main(String[] args)throws Exception {
  Map<String,ClassDef> base=read(args[0]),now=read(args[1]),overlay=read(args[2]),helper=read(args[3]);
  Set<String> expected=new TreeSet<>();String prefix="Lcom/shilapi/xcertplay/";
  for(String name:new String[]{"patch/EnhancementPanel","patch/CarConnectInfo","patch/AvDiagnostics","patch/OemSppPause","legacy/LegacyConnectionScreen","legacy/LegacySessionService"})expected.add(prefix+name+";");
  if(!expected.equals(overlay.keySet()))throw new Exception("Unexpected UI overlay");
  for(String name:helper.keySet()){
   boolean allowed=false;
   for(String type:new String[]{"Bc03Access","Bc03Profiles","OemTransport","SppRecoverySettings","SppStateTrace","SppIdleRecovery","OemTraceJournal","OemTrace","OemDiagnosticProvider","ConnectionDiagnostics","StandbyPolicy","StandbyRecovery","StandbySppRelease","ReportFiles","SppRecoveryPrefs","ManualSppRecovery","ManualRecovery","AllLogsFiles","AllLogsExport","OemSocketExit","FirstFramePolicy","FirstFrameRecovery","FirstFrameSettings"})
    if(name.equals(prefix+"patch/"+type+";")||name.startsWith(prefix+"patch/"+type+"$"))allowed=true;
   if(!allowed)throw new Exception("Compile-only dependency leaked "+name);
  }
  Map<String,ClassDef> permitted=new TreeMap<>(overlay);permitted.putAll(helper);int preserved=0,replaced=0,added=0;
  for(String type:base.keySet()){
   ClassDef result=now.get(type);if(result==null)throw new Exception("Baseline class removed "+type);
   if(permitted.containsKey(type)){if(!Audit.canonical(result).equals(Audit.canonical(permitted.get(type))))throw new Exception("Replacement differs "+type);replaced++;}
   else{if(!Audit.canonical(base.get(type)).equals(Audit.canonical(result)))throw new Exception("Unrelated class changed "+type);preserved++;}
  }
  for(String type:now.keySet())if(!base.containsKey(type)){if(!permitted.containsKey(type))throw new Exception("Unexpected new class "+type);added++;}
  System.out.println("Preserved baseline classes="+preserved+" intended replacements="+replaced+" authored additions="+added);
  ClassDef policy=now.get(prefix+"patch/FirstFramePolicy;");boolean field=false,deadline=false;
  for(Field f:policy.getFields())if(f.getName().equals("WAIT_MS"))field=((com.android.tools.smali.dexlib2.iface.value.LongEncodedValue)f.getInitialValue()).getValue()==30000L;
  for(Method m:policy.getMethods())if(m.getName().equals("check"))for(com.android.tools.smali.dexlib2.iface.instruction.Instruction i:m.getImplementation().getInstructions())
   if(i instanceof com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction && ((com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction)i).getWideLiteral()==30000L)deadline=true;
  if(!field||!deadline)throw new Exception("APK first-frame deadline is not 30000 milliseconds");
  System.out.println("APK first-frame deadline verified 30000ms. Audio/video rendering, fullscreen, wheel, USB, iAP2 protocol, saved-phone and SPP retry gate bytecode retained.");
 }
}
