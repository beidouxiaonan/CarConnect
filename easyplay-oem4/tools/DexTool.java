import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.smali.*;

public class DexTool {
  public static void main(String[] a) throws Exception {
    if (a[0].equals("dis")) {
      DexFile dex = DexFileFactory.loadDexFile(a[1], Opcodes.forApi(19));
      BaksmaliOptions opts = new BaksmaliOptions(); opts.apiLevel = 19;
      if (!Baksmali.disassembleDexFile(dex, new File(a[2]), 2, opts,
          Arrays.asList(Arrays.copyOfRange(a, 3, a.length)))) throw new Exception("disassemble failed");
    } else if (a[0].equals("asm")) {
      SmaliOptions opts = new SmaliOptions(); opts.apiLevel = 19; opts.jobs = 2; opts.outputDexFile = a[2];
      if (!Smali.assemble(opts, a[1])) throw new Exception("assemble failed");
    } else if (a[0].equals("merge")) {
      Map<String,ClassDef> classes = new TreeMap<>();
      int replaced = 0;
      for (int i=2; i<a.length; i++) {
        for (ClassDef c : DexFileFactory.loadDexFile(a[i], Opcodes.forApi(19)).getClasses()) {
          if (classes.put(c.getType(), c) != null) replaced++;
        }
      }
      DexFileFactory.writeDexFile(a[1], new ImmutableDexFile(Opcodes.forApi(19), classes.values()));
      System.out.println("classes=" + classes.size() + " replaced=" + replaced);
    }
  }
}
