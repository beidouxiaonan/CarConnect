import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;

/** Move the standalone report provider into primary DEX before MultiDex is attempted. */
public class UnifiedDex {
    static final String PROVIDER="Lcom/shilapi/xcertplay/patch/OemDiagnosticProvider;";
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(path,Opcodes.forApi(19)).getClasses())result.put(c.getType(),c);
        return result;
    }
    static void write(String path,Map<String,ClassDef> classes)throws Exception {
        DexFileFactory.writeDexFile(path,new ImmutableDexFile(Opcodes.forApi(19),classes.values()));
    }
    public static void main(String[] args)throws Exception {
        String work=args[0];Map<String,ClassDef> primary=read(work+"/original-classes.dex"),secondary=read(work+"/original-classes3.dex");
        ClassDef provider=secondary.remove(PROVIDER);
        if(provider==null||primary.containsKey(PROVIDER))throw new Exception("Unexpected provider placement");
        primary.put(PROVIDER,provider);
        write(work+"/base-classes.dex",primary);write(work+"/source-classes3.dex",secondary);
        System.out.println("Standalone diagnostic provider moved intact into primary DEX; removed from secondary DEX");
    }
}
