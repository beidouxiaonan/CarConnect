package com.shilapi.xcertplay.patch;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SocketExitTest {
    static OemSocketExit.Guard guard(final AtomicBoolean closed){return new OemSocketExit.Guard(){
        public void check()throws IOException{if(closed.get())throw new IOException("closed");}
    };}
    static OemSocketExit.Receipt receipt(final AtomicInteger bytes){return new OemSocketExit.Receipt(){
        public void received(int n){bytes.addAndGet(n);}
    };}
    @Test public void latePacketAfterCloseCannotReachProtocolOrChangeReceipt()throws Exception{
        final AtomicBoolean closed=new AtomicBoolean();final AtomicInteger bytes=new AtomicInteger();
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        InputStream source=new InputStream(){public int read()throws IOException{
            entered.countDown();try{release.await();}catch(InterruptedException e){throw new IOException(e);}return 42;
        }};
        final InputStream input=OemSocketExit.input(source,guard(closed),receipt(bytes));
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();final AtomicInteger result=new AtomicInteger(-2);
        Thread reader=new Thread(new Runnable(){public void run(){try{result.set(input.read());}catch(Throwable e){error.set(e);}}});
        reader.start();try{assertTrue(entered.await(2,TimeUnit.SECONDS));closed.set(true);release.countDown();reader.join(2000);
            assertFalse(reader.isAlive());assertTrue(error.get() instanceof IOException);assertEquals(-2,result.get());assertEquals(0,bytes.get());
        }finally{release.countDown();reader.join(2000);}
    }
    @Test public void closedStreamDoesNotStartAnotherNativeRead()throws Exception{
        AtomicBoolean closed=new AtomicBoolean(true);final AtomicInteger reads=new AtomicInteger(),bytes=new AtomicInteger();
        InputStream source=new InputStream(){public int read(){reads.incrementAndGet();return 1;}};
        InputStream input=OemSocketExit.input(source,guard(closed),receipt(bytes));
        try{input.read(new byte[2]);fail();}catch(IOException expected){}
        assertEquals(0,reads.get());assertEquals(0,bytes.get());
    }
    @Test public void openStreamPreservesBytesAndReceiptCounts()throws Exception{
        AtomicInteger bytes=new AtomicInteger();InputStream input=OemSocketExit.input(new ByteArrayInputStream(new byte[]{1,2,3}),guard(new AtomicBoolean()),receipt(bytes));
        byte[] data=new byte[5];assertEquals(2,input.read(data,1,2));assertEquals(1,data[1]);assertEquals(2,data[2]);
        assertEquals(3,input.read());assertEquals(-1,input.read());assertEquals(3,bytes.get());
    }
    @Test public void shutdownWakesBlockedReaderBeforeDescriptorClose()throws Exception{
        final CountDownLatch entered=new CountDownLatch(1),shutdown=new CountDownLatch(1);
        final AtomicBoolean stopped=new AtomicBoolean();final List<String> calls=new ArrayList<String>();
        Thread reader=new Thread(new Runnable(){public void run(){entered.countDown();try{shutdown.await();stopped.set(true);}catch(InterruptedException e){}}});
        reader.start();try{assertTrue(entered.await(2,TimeUnit.SECONDS));
            OemSocketExit.close(new OemSocketExit.Socket(){
                public void shutdownInput(){calls.add("in");shutdown.countDown();}
                public void shutdownOutput(){calls.add("out");}
                public void close(){calls.add("close");}
            },new OemSocketExit.Trace(){public void error(String op,IOException e){fail(op);}});
            reader.join(2000);assertTrue(stopped.get());assertEquals(Arrays.asList("in","out","close"),calls);
        }finally{shutdown.countDown();reader.join(2000);}
    }
    @Test public void failedShutdownStillClosesBothDirectionsAndDescriptor()throws Exception{
        final List<String> calls=new ArrayList<String>();
        OemSocketExit.close(new OemSocketExit.Socket(){
            public void shutdownInput()throws IOException{calls.add("in");throw new IOException("not connected");}
            public void shutdownOutput()throws IOException{calls.add("out");throw new IOException("not connected");}
            public void close(){calls.add("close");}
        },new OemSocketExit.Trace(){public void error(String op,IOException e){calls.add(op+" error");}});
        assertEquals(Arrays.asList("in","shutdownInput error","out","shutdownOutput error","close"),calls);
    }
    @Test public void descriptorFailureIsNotReportedAsSuccessfulClose()throws Exception{
        try{OemSocketExit.close(new OemSocketExit.Socket(){
            public void shutdownInput(){}public void shutdownOutput(){}
            public void close()throws IOException{throw new IOException("close failure");}
        },new OemSocketExit.Trace(){public void error(String op,IOException e){}});fail();
        }catch(IOException expected){assertEquals("close failure",expected.getMessage());}
    }
}
