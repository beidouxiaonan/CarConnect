package com.shilapi.xcertplay.patch;
import java.io.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecoveryExportTest {
    @Test public void manualHandoffCanSuppressCleanupForOnlyTheNextSamePhoneRun(){
        ManualSppRecovery.handoff("phone-a");assertTrue(ManualSppRecovery.consumeHandoff("phone-a"));assertFalse(ManualSppRecovery.consumeHandoff("phone-a"));
    }
    @Test public void differentPhoneCannotInheritManualHandoff(){
        ManualSppRecovery.handoff("phone-a");assertFalse(ManualSppRecovery.consumeHandoff("phone-b"));assertFalse(ManualSppRecovery.consumeHandoff("phone-a"));
    }
    static class Model implements SppRelease.NativePort,SppRelease.Monitor,SppRelease.Time,SppRelease.Control,SppIdleRecovery.Port,SppIdleRecovery.Clock {
        long now;int requests,nativeRequests,begins,clears;boolean busy=true,pending=true,nativeOn,cancel,failCommit,releaseOnRequest=true;
        public long now(){return now;}public void pause(){now+=100000000L;}
        public void check()throws Exception{if(cancel&&now>=400000000L)throw new IOException("cancel");}
        public void log(String s){}
        public boolean connected(){return busy;}
        public void disconnect(){requests++;if(releaseOnRequest)busy=false;}
        public void disconnectAll(){nativeRequests++;}
        public boolean pending(){return pending;}public boolean nativeEnabled(){return nativeOn;}
        public void beforeRequest()throws Exception{if(failCommit)throw new IOException("disk full");begins++;pending=true;}
        public void released(){clears++;pending=false;}
        public void validate()throws Exception{check();}
        public void clearJournal(){released();}
    }
    @Test public void explicitPermitWorksWithoutPreClearingDurableJournal()throws Exception{
        Model m=new Model();assertTrue(ManualSppRecovery.run(new SppRelease(),m,m,m,m));
        assertEquals(1,m.begins);assertEquals(1,m.requests);assertEquals(0,m.nativeRequests);assertEquals(0,m.clears);assertTrue(m.pending);
        SppIdleRecovery.confirm(m,m);assertEquals(1,m.clears);assertFalse(m.pending);
    }
    @Test public void failedOneShotKeepsPendingAndDoesNotPermitNormalRetry()throws Exception{
        Model m=new Model();m.releaseOnRequest=false;SppRelease r=new SppRelease();
        try{ManualSppRecovery.run(r,m,m,m,m);fail();}catch(SppRelease.BlockedException expected){}
        assertTrue(m.pending);assertEquals(1,m.requests);assertEquals(0,m.clears);
        try{r.ensureFree(true,false,m,m,m,m);fail();}catch(SppRelease.BlockedException expected){}
        assertEquals(1,m.requests);
    }
    @Test public void pendingCommitFailurePreventsEveryDispatch()throws Exception{
        Model m=new Model();m.failCommit=true;
        try{ManualSppRecovery.run(new SppRelease(),m,m,m,m);fail();}catch(IOException expected){}
        assertEquals(0,m.requests);assertEquals(0,m.nativeRequests);assertTrue(m.pending);
    }
    @Test public void idlePermitDoesNotDisconnect()throws Exception{
        Model m=new Model();m.busy=false;assertFalse(ManualSppRecovery.run(new SppRelease(),m,m,m,m));assertEquals(0,m.requests);
        assertTrue(m.pending);SppIdleRecovery.confirm(m,m);assertFalse(m.pending);
    }
    @Test public void cancelledFinalConfirmationKeepsOldPendingEvidence()throws Exception{
        Model m=new Model();ManualSppRecovery.run(new SppRelease(),m,m,m,m);m.cancel=true;
        try{SppIdleRecovery.confirm(m,m);fail();}catch(IOException expected){}assertEquals(0,m.clears);assertTrue(m.pending);
    }
    @Test public void BusyAgainBeforeFinalConfirmationDoesNotClearJournal()throws Exception{
        Model m=new Model();ManualSppRecovery.run(new SppRelease(),m,m,m,m);m.busy=true;
        try{SppIdleRecovery.confirm(m,m);fail();}catch(IOException expected){}assertEquals(0,m.clears);assertTrue(m.pending);assertEquals(1,m.requests);
    }
    @Test public void experimentIsBoundedToOneNativeRequest()throws Exception{
        Model m=new Model();m.nativeOn=true;m.releaseOnRequest=false;
        try{ManualSppRecovery.run(new SppRelease(),m,m,m,m);fail();}catch(SppRelease.BlockedException expected){}
        assertEquals(1,m.requests);assertEquals(1,m.nativeRequests);assertTrue(m.pending);assertEquals(0,m.clears);
    }
    @Test public void cancellationDoesNotClearFailedJournal()throws Exception{
        Model m=new Model();m.cancel=true;m.releaseOnRequest=false;
        try{ManualSppRecovery.run(new SppRelease(),m,m,m,m);fail();}catch(IOException expected){}
        assertEquals(1,m.requests);assertTrue(m.pending);assertEquals(0,m.clears);
    }
    @Test public void manualPermitStillHonorsPreviousCleanupCooldown()throws Exception{
        Model m=new Model();m.releaseOnRequest=false;SppRelease r=new SppRelease();
        try{ManualSppRecovery.run(r,m,m,m,m);fail();}catch(IOException expected){}
        m.releaseOnRequest=true;ManualSppRecovery.run(r,m,m,m,m);assertTrue(m.now>=SppRelease.COOLDOWN);assertEquals(2,m.requests);
    }
    @Test public void exportWritesUtf8AndNeverOverwritesSameTimestamp()throws Exception{
        Path dir=Files.createTempDirectory("all-logs-test-");
        try{
            File a=AllLogsFiles.save("中文日志\nfirst",dir.toFile(),1791702000000L);
            File b=AllLogsFiles.save("second",dir.toFile(),1791702000000L);
            assertNotEquals(a,b);assertEquals("中文日志\nfirst",new String(Files.readAllBytes(a.toPath()),"UTF-8"));
            assertEquals(2,dir.toFile().list().length);
        }finally{for(File f:dir.toFile().listFiles())f.delete();Files.delete(dir);}
    }
    @Test public void invalidDestinationDoesNotSilentlyFallback()throws Exception{
        Path f=Files.createTempFile("all-logs-file-",".txt");
        try{try{AllLogsFiles.save("report",f.toFile(),1791702000000L);fail();}catch(IOException expected){}
            assertEquals(0,Files.size(f));}finally{Files.delete(f);}
    }
    @Test public void reportRedactionPreservesWholeLongHistoryAndFirmwareHashes(){
        String hash=Bc03Profiles.DAEMON_SHA,body=new String(new char[90000]).replace('\0','x');
        String text=AllLogsFiles.redact("6C:1F:8A:98:4E:D4\npassword=abc123\nsha="+hash+"\n"+body);
        assertFalse(text.contains("6C:1F"));assertFalse(text.contains("abc123"));assertTrue(text.contains(hash));assertTrue(text.endsWith(body));
    }
    @Test public void onlyKnownReportFilesMayBeRead()throws Exception{
        Path dir=Files.createTempDirectory("all-logs-sources-");
        try{try{AllLogsFiles.readKnown(dir.toFile(),"../keys.txt");fail();}catch(IOException expected){}
            assertTrue(AllLogsFiles.readKnown(dir.toFile(),"legacy-session-status.txt").contains("暂无"));
        }finally{Files.delete(dir);}
    }
    @Test public void destinationRequiresAbsoluteNonRootPath()throws Exception{
        for(String p:new String[]{"relative-folder","/","/dev/pts/1","/system/a","/data/user/0","\0bad"}){
            try{AllLogsFiles.folder(p);fail(p);}catch(IOException expected){}
        }
    }
}
