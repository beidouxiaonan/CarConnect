package com.shilapi.xcertplay.patch;
import java.io.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class StandbyReportTest {
    @Test public void duplicateOffAndWakeCannotStartAnotherTask(){
        StandbyPolicy p=new StandbyPolicy();long n=p.off();assertTrue(n>0);assertEquals(0,p.off());
        assertEquals(n,p.wake());assertEquals(0,p.wake());assertTrue(p.blocked());assertTrue(p.complete(n));assertFalse(p.complete(n));assertFalse(p.blocked());
    }
    @Test public void NewOffCancelsOldWakeAndDisablingInvalidatesAllWork(){
        StandbyPolicy p=new StandbyPolicy();long old=p.off();p.wake();long next=p.off();
        assertFalse(p.wakeValid(old));assertFalse(p.complete(old));assertTrue(p.offValid(next));
        p.cancel();assertFalse(p.offValid(next));assertFalse(p.blocked());
    }
    @Test public void WakeWithoutOffCannotRestartAnOrdinaryConnection(){assertEquals(0,new StandbyPolicy().wake());}
    @Test public void fallbackSavesCompleteUtf8WithoutAFileSharingApplication()throws Exception{
        Path root=Files.createTempDirectory("carconnect-report-");File denied=root.resolve("file-instead-of-directory").toFile();denied.createNewFile();
        File fallback=root.resolve("fallback").toFile();String report="SCREEN_OFF\n中文\nWAKE idle=false";
        try{File saved=ReportFiles.save(report,"CarConnect-OEM-1790000000000.txt",denied,fallback);
            assertEquals(report,new String(Files.readAllBytes(saved.toPath()),"UTF-8"));
            assertFalse(new File(fallback,saved.getName()+".tmp").exists());
        }finally{if(fallback.isDirectory())for(File f:fallback.listFiles())f.delete();fallback.delete();denied.delete();root.toFile().delete();}
    }
    @Test public void copyingLongLogKeepsBeginningAndLatestFailureBounded(){
        String full="PROCESS_BEGIN"+new String(new char[90000]).replace('\0','中')+"LATEST_SPP_FAILURE";
        String copy=ReportFiles.excerpt(full);assertTrue(copy.startsWith("PROCESS_BEGIN"));assertTrue(copy.endsWith("LATEST_SPP_FAILURE"));assertTrue(copy.length()<=60000);assertTrue(copy.contains("中段省略"));
    }
    @Test public void noWritableDirectoryReportsFailure()throws Exception{
        try{ReportFiles.save("log","CarConnect-OEM-1790000000000.txt",(File)null);fail();}catch(IOException expected){}
    }
    @Test public void nameCannotEscapeFixedDirectory()throws Exception{
        try{ReportFiles.save("log","../other.txt",new File("."));fail();}catch(IOException expected){}
    }
    @Test public void retentionOnlyPrunesOurReportNames()throws Exception{
        File d=Files.createTempDirectory("carconnect-retain-").toFile();File user=new File(d,"my-file.txt");user.createNewFile();
        try{for(int n=0;n<8;n++)ReportFiles.save("log","CarConnect-OEM-179000000000"+n+".txt",d);
            assertTrue(user.isFile());assertEquals(6,d.listFiles().length);
        }finally{for(File f:d.listFiles())f.delete();d.delete();}
    }
}
