package com.shilapi.xcertplay.patch;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class OemSocketOutputTest {
    private static class G implements OemSocketOutput.Guard {
        boolean closed;String reason="original cancellation";int closes;
        public void check()throws IOException {if(closed)throw new IOException(reason);}
        public void close(){closed=true;closes++;}
    }
    private static class Direct extends ByteArrayOutputStream {
        int drains;
        public void flush()throws IOException {drains++;throw new IOException("ioctl failed: EBADF");}
    }
    @Test public void macWriteDoesNotWaitForPeerDrain()throws Exception {
        Direct d=new Direct();G g=new G();OemSocketOutput s=new OemSocketOutput(d,g);
        s.write(OemProtocol.socketHello("AA:BB:CC:DD:EE:FF"));s.flush();
        assertEquals("AABBCCDDEEFF",d.toString("US-ASCII"));assertEquals(0,d.drains);
    }
    @Test public void upperBufferedStreamStillSendsItsBuffer()throws Exception {
        Direct d=new Direct();BufferedOutputStream b=new BufferedOutputStream(new OemSocketOutput(d,new G()));
        b.write(new byte[]{1,2,3});assertEquals(0,d.size());b.flush();assertArrayEquals(new byte[]{1,2,3},d.toByteArray());assertEquals(0,d.drains);
    }
    @Test public void closedOwnerFlushStillFails()throws Exception {
        G g=new G();g.closed=true;try{new OemSocketOutput(new Direct(),g).flush();fail();}catch(IOException e){assertEquals(g.reason,e.getMessage());}
    }
    @Test public void cancelledOwnerIsNotHiddenByBadDescriptor()throws Exception {
        final G g=new G();OutputStream d=new OutputStream(){public void write(int v)throws IOException {g.closed=true;throw new IOException("EBADF");}};
        try{new OemSocketOutput(d,g).write(1);fail();}catch(IOException e){assertEquals(g.reason,e.getMessage());}
    }
    @Test public void actualWriteFailureRemainsVisible()throws Exception {
        OutputStream d=new OutputStream(){public void write(int v)throws IOException {throw new IOException("EPIPE");}};
        try{new OemSocketOutput(d,new G()).write(1);fail();}catch(IOException e){assertEquals("EPIPE",e.getMessage());}
    }
    @Test public void closedOwnerDoesNotWriteMoreBytes()throws Exception {
        G g=new G();g.closed=true;Direct d=new Direct();try{new OemSocketOutput(d,g).write(new byte[]{1});fail();}catch(IOException e){}assertEquals(0,d.size());
    }
    @Test public void closeUsesOwnerAndDoesNotDrain()throws Exception {
        G g=new G();Direct d=new Direct();new OemSocketOutput(d,g).close();assertEquals(1,g.closes);assertEquals(0,d.drains);
    }
    @Test public void cancellationImmediatelyAfterWriteIsReported()throws Exception {
        final G g=new G();Direct d=new Direct(){public void write(byte[] b,int off,int len){super.write(b,off,len);g.closed=true;}};
        try{new OemSocketOutput(d,g).write(new byte[]{1});fail();}catch(IOException e){assertEquals(g.reason,e.getMessage());}assertEquals(1,d.size());
    }
}
