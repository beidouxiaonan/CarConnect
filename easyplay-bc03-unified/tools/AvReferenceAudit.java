import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;

/** Compares complete AV helper classes and their actual APK integration with a released reference. */
public class AvReferenceAudit {
    static final String R="Lcom/shilapi/xcertplay/",P=R+"patch/";
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> r=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(path,Opcodes.forApi(19)).getClasses())r.put(c.getType(),c);
        return r;
    }
    static String canonical(ClassDef c)throws Exception {
        if(c==null)throw new Exception("Missing AV class");
        BaksmaliOptions o=new BaksmaliOptions();o.apiLevel=19;o.sequentialLabels=true;
        StringWriter s=new StringWriter();BaksmaliWriter w=new BaksmaliWriter(s);
        ClassDefinition d=new ClassDefinition(o,c);d.writeTo(w);w.flush();
        if(d.hadValidationErrors())throw new Exception("Invalid class "+c.getType());
        return s.toString().replace("\r\n","\n").replace("const-string/jumbo ","const-string ")
            .replaceAll("(?m)^    nop\\n\\n(?=    :(?:pswitch|sswitch)_data_)","")
            .replaceAll("(?m)^(\\.field [^\\r\\n]+?) = (false|0x0|null|0\\.0f?)(?=\\r?$)","$1");
    }
    static String diagnostic(String s)throws Exception {
        Matcher m=Pattern.compile("(?ms)^\\.method [^\\n]* showDiagnostics\\(Landroid/app/Activity;\\)V\\n.*?^\\.end method").matcher(s);
        if(!m.find())throw new Exception("Missing AV diagnostics integration");
        String result=m.group();if(m.find())throw new Exception("Duplicate diagnostics");return result;
    }
    static String hash(String text)throws Exception {
        byte[] b=MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"));
        StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.US,"%02x",v&255));return s.toString();
    }
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> reference=read(args[0]),current=read(args[1]);int equal=0;
        for(String t:new String[]{R+"media/LegacyAudioRenderer;",R+"airplay/AudioStream;",R+"legacy/LegacyActivity;",
                P+"PcmBufferState;",P+"StableAudioTrack;",P+"AudioRuntime;",P+"FullscreenRecovery;",
                P+"FullscreenRecovery$Controller;",P+"AvDiagnostics;",P+"AvDiagnostics$1;"}) {
            String before=canonical(reference.get(t)),after=canonical(current.get(t));
            if(!before.equals(after))throw new Exception("AV reference mismatch: "+t+" old="+hash(before)+" current="+hash(after));
            System.out.println("MATCH "+t+" SHA256="+hash(after));equal++;
        }
        String before=diagnostic(canonical(reference.get(P+"EnhancementPanel;"))),after=diagnostic(canonical(current.get(P+"EnhancementPanel;")));
        // The sole supported firmware wording change; no method instructions are ignored.
        String label="BC03 1.7.9 / 1.3.6 / gocsdk";
        if(before.indexOf(label)!=before.lastIndexOf(label)||before.indexOf(label)<0)throw new Exception("Unexpected firmware wording occurrences");
        before=before.replace(label,"BC03 1.7.2 / 1.7.9 / 1.3.6 / gocsdk");
        if(!before.equals(after)){
            String[] a=before.split("\n"),b=after.split("\n");int shown=0;
            for(int i=0;i<Math.min(a.length,b.length)&&shown<6;i++)if(!a[i].equals(b[i])){System.out.println("DIFF old="+a[i]+" current="+b[i]);shown++;}
            throw new Exception("AV diagnostics method mismatch");
        }
        System.out.println("MATCH EnhancementPanel.showDiagnostics SHA256="+hash(after));
        System.out.println("Complete released AV classes matched="+equal+"; diagnostics method matched=1 (one firmware wording substitution)");
    }
}
