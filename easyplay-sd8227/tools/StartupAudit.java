import java.io.*;
import java.util.*;
import java.util.regex.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;

public class StartupAudit {
    static String canonical(ClassDef c) throws Exception {
        BaksmaliOptions o=new BaksmaliOptions(); o.apiLevel=19; o.sequentialLabels=true;
        StringWriter s=new StringWriter(); BaksmaliWriter w=new BaksmaliWriter(s);
        ClassDefinition d=new ClassDefinition(o,c); d.writeTo(w); w.flush();
        if(d.hadValidationErrors()) throw new Exception("Invalid class "+c.getType());
        return s.toString().replace("\r\n","\n").replace("const-string/jumbo ","const-string ")
            .replaceAll("(?m)^    nop\\n\\n(?=    :(?:pswitch|sswitch)_data_)","")
            .replaceAll("(?m)^(\\.field [^\\r\\n]+?) = (false|0x0|null|0\\.0f?)(?=\\r?$)","$1");
    }
    static Map<String,String> methods(String s) {
        Map<String,String> out=new TreeMap<>();
        Matcher m=Pattern.compile("(?m)^\\.method ([^\\n]+)\\n.*?^\\.end method",Pattern.DOTALL).matcher(s);
        while(m.find()) out.put(m.group(1).trim(),m.group()); return out;
    }
    static void check(String base,String next,boolean primary) throws Exception {
        Map<String,ClassDef> now=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(next,Opcodes.forApi(19)).getClasses()) now.put(c.getType(),c);
        int unchanged=0;
        for(ClassDef c:DexFileFactory.loadDexFile(base,Opcodes.forApi(19)).getClasses()) {
            ClassDef n=now.remove(c.getType()); if(n==null) throw new Exception("Removed "+c.getType());
            String before=canonical(c),after=canonical(n);
            if(before.equals(after)){unchanged++;continue;}
            if(primary && c.getType().equals("Landroidx/multidex/MultiDex;")) {
                String oldCall="Landroidx/multidex/MultiDex$V19;->install(Ljava/lang/ClassLoader;Ljava/util/List;Ljava/io/File;)V";
                String newCall="Lio/github/beidouxiaonan/carconnect/startup/LegacyDexInstaller;->install(Ljava/lang/ClassLoader;Ljava/util/List;Ljava/io/File;)V";
                if(before.indexOf(oldCall) < 0 || before.indexOf(oldCall) != before.lastIndexOf(oldCall)
                    || !before.replace(oldCall,newCall).equals(after)) throw new Exception("Unexpected MultiDex change");
                System.out.println("MultiDex: exactly one installer call replaced; extractor/retry logic unchanged");
                continue;
            }
            if(primary || !c.getType().equals("Lcom/shilapi/xcertplay/legacy/LegacyActivity;")) throw new Exception("Unexpected modification "+c.getType());
            String meta=after.substring(0,after.indexOf(".method ")).replace(".field private sdStartupFailed:Z\n","");
            if(!meta.replaceAll("(?m)^[ \\t]*\\n", "").trim().equals(before.substring(0,before.indexOf(".method ")).replaceAll("(?m)^[ \\t]*\\n", "").trim())) throw new Exception("Changed activity metadata");
            Map<String,String> bm=methods(before),nm=methods(after);
            for(String key:bm.keySet()) {
                String dest=key;
                if(key.equals("protected onCreate(Landroid/os/Bundle;)V")) dest="private final sdOriginalOnCreate(Landroid/os/Bundle;)V";
                if(key.equals("protected onStart()V")) dest="private final sdOriginalOnStart()V";
                String value=nm.remove(dest);
                String expected=bm.get(key).replace(".method "+key+"\n",".method "+dest+"\n");
                if(!expected.equals(value)) throw new Exception("Original method changed "+key);
            }
            if(!nm.keySet().equals(new TreeSet<>(Arrays.asList("protected onCreate(Landroid/os/Bundle;)V","protected onStart()V","protected onResume()V")))) throw new Exception("Unexpected wrapper method set");
        }
        if(!primary && !now.isEmpty()) throw new Exception("Added secondary classes");
        if(primary) {
            if(now.isEmpty()) throw new Exception("Bootstrap classes absent");
            for(String type:now.keySet()) if(!type.startsWith("Lio/github/beidouxiaonan/carconnect/startup/")) throw new Exception("Foreign helper "+type);
            for(String type:new String[]{"StartupApplication","EntryActivity","StartupLog","StartupState","LegacyDexInstaller"})
                if(!now.containsKey("Lio/github/beidouxiaonan/carconnect/startup/"+type+";")) throw new Exception("Missing primary helper "+type);
        }
        System.out.println((primary?"Primary":"Secondary")+": "+unchanged+" existing classes unchanged; "+now.size()+" primary bootstrap helpers");
    }
    public static void main(String[] a) throws Exception {
        check(a[0]+"/base-classes.dex",a[0]+"/patched-classes.dex",true);
        check(a[0]+"/base-classes3.dex",a[0]+"/patched-classes3.dex",false);
    }
}
