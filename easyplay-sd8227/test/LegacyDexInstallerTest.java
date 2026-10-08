import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import io.github.beidouxiaonan.carconnect.startup.LegacyDexInstaller;

/** JVM doubles test reflection dispatch and atomic classpath append, not Dalvik dexopt. */
public class LegacyDexInstallerTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private File archive;
    private List<File> files;
    private List<String> log;
    @Before public void setup() throws Exception {
        archive = temp.newFile("secondary.zip"); files = Arrays.asList(archive);
        log = new ArrayList<String>();
        LegacyDexInstaller.setReporter(new LegacyDexInstaller.Reporter() {
            public void stage(String message) { log.add(message); }
        });
    }
    static class Element {
        final String id;
        private final Object dexFile;
        Element(String id) { this(id, new Object()); }
        Element(String id, Object dex) { this.id = id; this.dexFile = dex; }
    }
    static class Path {
        private Element[] dexElements = new Element[] {new Element("primary")};
        Element[] elements() { return dexElements; }
    }
    static class LoaderBase extends ClassLoader {
        private Object pathList;
        LoaderBase(Object path) { pathList = path; }
    }
    static class Loader extends LoaderBase { Loader(Object path) { super(path); } }
    static class OldPath extends Path {
        int calls;
        private Element[] makeDexElements(ArrayList<File> files, File output) {
            calls++; return new Element[] {new Element("secondary")};
        }
    }
    static class KitKatPath extends Path {
        private static Element[] makeDexElements(ArrayList<File> files, File output, ArrayList<IOException> errors) {
            return new Element[] {new Element("kitkat")};
        }
    }
    static class VendorListPath extends Path {
        private Element[] makeDexElements(List<File> files, File output, List<IOException> errors) {
            return new Element[] {new Element("list")};
        }
    }
    static class VendorFourPath extends Path {
        ClassLoader supplied;
        private Element[] makeDexElements(List<File> files, File output, List<IOException> errors, ClassLoader loader) {
            supplied=loader; return new Element[] {new Element("four")};
        }
    }
    static class BothPath extends Path {
        int oldCalls, newCalls;
        private Element[] makeDexElements(ArrayList<File> files, File output) {
            oldCalls++; return new Element[] {new Element("old")};
        }
        private Element[] makeDexElements(ArrayList<File> files, File output, ArrayList<IOException> errors) {
            newCalls++; return new Element[] {new Element("new")};
        }
    }
    static class ErrorPath extends Path {
        int oldCalls;
        private Element[] makeDexElements(ArrayList<File> files, File output) {
            oldCalls++; return new Element[] {new Element("wrong-fallback")};
        }
        private Element[] makeDexElements(ArrayList<File> files, File output, ArrayList<IOException> errors) {
            errors.add(new IOException("dexopt failed")); return new Element[] {new Element("partial")};
        }
    }
    static class ThrowPath extends Path {
        boolean runtime;
        private Element[] makeDexElements(ArrayList<File> files, File output) throws IOException {
            if(runtime) throw new IllegalStateException("VM failed");
            throw new IOException("cannot read DEX");
        }
    }
    static class BadPath extends Path {
        int mode;
        private Object[] makeDexElements(ArrayList<File> files, File output) {
            switch(mode) {
                case 0: return null;
                case 1: return new Element[0];
                case 2: return new Object[] {new Object()};
                case 3: return new Element[] {null};
                case 4: return new Element[] {new Element("zip-only",null)};
                default: return new Element[] {new Element("recovered")};
            }
        }
    }
    static class UnknownPath extends Path {
        int calls;
        private Element[] makeDexElements(String files, File output) { calls++; return new Element[0]; }
    }
    static class TransitionalPath {
        private Element[] pathElements = new Element[] {new Element("primary")};
        private Element[] makeDexElements(ArrayList<File> files, File output) { return new Element[] {new Element("secondary")}; }
    }
    private void install(Object path) throws Exception { LegacyDexInstaller.install(new Loader(path), files, temp.getRoot()); }
    private void good(Path path,String second) throws Exception {
        Element first=path.elements()[0]; install(path);
        assertEquals(2,path.elements().length); assertSame(first,path.elements()[0]);
        assertEquals(second,path.elements()[1].id);
        assertEquals(Element[].class,path.elements().getClass());
        assertTrue(log.get(log.size()-1).startsWith("DEX_APPEND_OK"));
    }
    @Test public void oldTwoArgumentFactoryWorksWithInheritedPrivateFields() throws Exception {
        OldPath path=new OldPath(); good(path,"secondary"); assertEquals(1,path.calls);
        assertTrue(log.toString().contains("java.util.ArrayList,java.io.File)"));
    }
    @Test public void kitkatStaticThreeArgumentFactoryPreservesPrimaryPrecedence() throws Exception { good(new KitKatPath(),"kitkat"); }
    @Test public void vendorListSignatureIsRecognizedWithoutUsingSdk() throws Exception { good(new VendorListPath(),"list"); }
    @Test public void fourArgumentFactoryReceivesActualLoader() throws Exception {
        VendorFourPath path=new VendorFourPath(); Loader loader=new Loader(path);
        LegacyDexInstaller.install(loader,files,temp.getRoot()); assertSame(loader,path.supplied);
        assertEquals(2,path.elements().length);
    }
    @Test public void errorReportingFactoryWinsWhenBothSignaturesExist() throws Exception {
        BothPath path=new BothPath(); good(path,"new"); assertEquals(0,path.oldCalls); assertEquals(1,path.newCalls);
    }
    @Test public void reportedDexoptErrorLeavesClasspathUntouchedAndDoesNotTryAnotherFactory() throws Exception {
        ErrorPath path=new ErrorPath(); Element[] before=path.elements();
        try {install(path);fail();} catch(IOException error) {assertEquals("dexopt failed",error.getCause().getMessage());}
        assertSame(before,path.elements()); assertEquals(0,path.oldCalls);
    }
    @Test public void directIoErrorIsUnwrappedForOriginalExtractorRetry() throws Exception {
        ThrowPath path=new ThrowPath(); Element[] before=path.elements();
        try {install(path);fail();} catch(IOException error) {assertEquals("cannot read DEX",error.getMessage());}
        assertSame(before,path.elements());
    }
    @Test public void runtimeFaultKeepsCauseAndDoesNotBecomeRecoverableIoError() throws Exception {
        ThrowPath path=new ThrowPath(); path.runtime=true; Element[] before=path.elements();
        try {install(path);fail();} catch(InvocationTargetException error) {assertTrue(error.getCause() instanceof IllegalStateException);}
        assertSame(before,path.elements());
    }
    @Test public void nullShortWrongTypeAndZipOnlyElementsNeverAppend() throws Exception {
        for(int mode=0;mode<5;mode++) {
            BadPath path=new BadPath(); path.mode=mode; Element[] before=path.elements();
            try {install(path);fail("mode "+mode);} catch(IOException expected) {}
            assertSame(before,path.elements());
        }
    }
    @Test public void unknownSignatureIsLoggedAndNeverInvoked() throws Exception {
        UnknownPath path=new UnknownPath(); Element[] before=path.elements();
        try {install(path);fail();} catch(NoSuchMethodException error) {assertTrue(error.getMessage().contains("java.lang.String"));}
        assertSame(before,path.elements()); assertEquals(0,path.calls);
        assertTrue(log.get(0).startsWith("DEX_FACTORY_AVAILABLE"));
    }
    @Test public void transitionalPathElementsFieldIsSupported() throws Exception {
        TransitionalPath path=new TransitionalPath(); Element first=path.pathElements[0]; install(path);
        assertEquals(2,path.pathElements.length); assertSame(first,path.pathElements[0]);
    }
    @Test public void emptyListDoesNotRequireDexCapableLoader() throws Exception {
        LegacyDexInstaller.install(new ClassLoader(){},Collections.<File>emptyList(),temp.getRoot()); assertTrue(log.isEmpty());
    }
    @Test public void missingExtractedArchiveDoesNotInvokeFactory() throws Exception {
        OldPath path=new OldPath(); archive.delete();
        try {install(path);fail();} catch(IOException expected) {} assertEquals(0,path.calls);
    }
    @Test public void extractorRetryAfterFailedAttemptAppendsOnlyOnce() throws Exception {
        BadPath path=new BadPath(); path.mode=4; Element[] before=path.elements();
        try {install(path);fail();} catch(IOException expected) {}
        assertSame(before,path.elements());
        // Simulate original extractor performing its single retry with a valid factory result.
        path.mode=5; install(path);
        assertEquals(2,path.elements().length); assertSame(before[0],path.elements()[0]);
        assertEquals("recovered",path.elements()[1].id);
    }
}
