package com.shilapi.xcertplay.patch;
import java.io.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class DiagnosticsTest {
    static class Sim implements SppIdleRecovery.Port,SppIdleRecovery.Clock {
        long time;int clears,validations,reads;boolean busy,cancel,changed,commitFails,flap;
        public long now(){return time;}
        public void pause(){time+=100000000L;}
        public void check()throws Exception {if(cancel&&time>=400000000L)throw new IOException("cancel");}
        public boolean connected(){reads++;return busy||flap&&time>=500000000L&&time<1300000000L;}
        public void validate()throws Exception {validations++;if(changed&&time>=600000000L)throw new IOException("phone changed");}
        public void clearJournal()throws Exception {if(commitFails)throw new IOException("disk failure");clears++;}
    }
    @Test public void freeStateMustRemainStableBeforeClearing()throws Exception {
        Sim s=new Sim();SppIdleRecovery.confirm(s,s);assertEquals(1,s.clears);assertEquals(600000000L,s.time);assertTrue(s.reads>=7);assertEquals(2,s.validations);
    }
    @Test public void occupiedStateDoesNotExpireProtection()throws Exception {
        Sim s=new Sim();s.busy=true;try{SppIdleRecovery.confirm(s,s);fail();}catch(IOException expected){}assertEquals(0,s.clears);assertEquals(3000000000L,s.time);
    }
    @Test public void intermittentBusyStateRestartsStabilityWindow()throws Exception {
        Sim s=new Sim();s.flap=true;SppIdleRecovery.confirm(s,s);assertEquals(1900000000L,s.time);assertEquals(1,s.clears);
    }
    @Test public void cancelledTaskCannotClearLater()throws Exception {
        Sim s=new Sim();s.cancel=true;try{SppIdleRecovery.confirm(s,s);fail();}catch(IOException expected){}assertEquals(0,s.clears);
    }
    @Test public void differentPhoneAtFinalValidationCannotClear()throws Exception {
        Sim s=new Sim();s.changed=true;try{SppIdleRecovery.confirm(s,s);fail();}catch(IOException expected){}assertEquals(0,s.clears);
    }
    @Test public void commitFailurePropagatesAndNeverSucceeds()throws Exception {
        Sim s=new Sim();s.commitFails=true;try{SppIdleRecovery.confirm(s,s);fail();}catch(IOException expected){}assertEquals(0,s.clears);
    }
    @Test public void cancellationDuringBlockedQueryIsRecheckedBeforeCommit()throws Exception {
        final Sim s=new Sim(){public boolean connected(){time+=400000000L;return false;}};s.cancel=true;
        try{SppIdleRecovery.confirm(s,s);fail();}catch(IOException expected){}assertEquals(0,s.clears);
    }
    @Test public void redactsMacFormsButPreservesFirmwareHashes(){
        String digest=Bc03Profiles.DAEMON_SHA;
        String t=OemTraceJournal.clean("phone=6C:1F:8A:98:4E:D4 module=001122AABBCC sha="+digest);
        assertFalse(t.contains("6C:1F"));assertFalse(t.contains("001122AABBCC"));assertTrue(t.contains(digest));
    }
    @Test public void diskWriterPersistsEvidenceAcrossJournalInstances()throws Exception {
        File dir=Files.createTempDirectory("carconnect-trace-test-").toFile();
        OemTraceJournal first=new OemTraceJournal(dir),second=null;
        try{
            first.event("BINDER isSppConnect=false phone=6C:1F:8A:98:4E:D4");
            File file=new File(dir,"current.txt");long deadline=System.nanoTime()+3000000000L;
            while(!file.isFile()||!new String(Files.readAllBytes(file.toPath()),"UTF-8").contains("[MAC]")){
                if(System.nanoTime()>deadline)fail("Writer did not persist evidence");Thread.sleep(20);
            }
            first.stopWriter();second=new OemTraceJournal(dir);String report=second.report();
            assertTrue(report.contains("isSppConnect=false"));assertFalse(report.contains("6C:1F"));assertTrue(report.contains("[MAC]"));
        }finally{first.stopWriter();if(second!=null)second.stopWriter();for(File f:dir.listFiles())f.delete();dir.delete();}
    }
    @Test public void traceRecordsAreBounded(){assertTrue(OemTraceJournal.clean(new String(new char[5000]).replace('\0','x')).length()<2100);}
}
