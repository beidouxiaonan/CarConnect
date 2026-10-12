package com.shilapi.xcertplay.patch;
import java.io.*;

/** Finish the owned socket in both directions before dropping its descriptor. */
final class OemSocketExit {
    interface Socket extends Closeable {
        void shutdownInput() throws IOException;
        void shutdownOutput() throws IOException;
    }
    interface Trace { void error(String operation, IOException error); }
    static void close(Socket socket, Trace trace) throws IOException {
        // Old Android native reads can retain the socket after close(fd). Shutdown wakes that read.
        try { socket.shutdownInput(); } catch(IOException error) { trace.error("shutdownInput",error); }
        try { socket.shutdownOutput(); } catch(IOException error) { trace.error("shutdownOutput",error); }
        socket.close();
    }
    interface Guard { void check() throws IOException; }
    interface Receipt { void received(int count) throws IOException; }
    static InputStream input(final InputStream source, final Guard guard, final Receipt receipt) {
        return new FilterInputStream(source) {
            public int read(byte[] bytes,int offset,int length) throws IOException {
                guard.check();int count=in.read(bytes,offset,length);guard.check();
                if(count>0)receipt.received(count);return count;
            }
            public int read() throws IOException {
                guard.check();int value=in.read();guard.check();
                if(value>=0)receipt.received(1);return value;
            }
            public void close() throws IOException { source.close(); }
        };
    }
}
