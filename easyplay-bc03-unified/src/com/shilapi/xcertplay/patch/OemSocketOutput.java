package com.shilapi.xcertplay.patch;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/** Only for LocalSocket's unbuffered native output. Do not wrap a buffered stream. */
final class OemSocketOutput extends OutputStream {
    interface Guard extends Closeable {void check()throws IOException;}
    private final OutputStream direct;
    private final Guard guard;
    OemSocketOutput(OutputStream direct,Guard guard){this.direct=direct;this.guard=guard;}
    public void write(byte[] b,int off,int len)throws IOException {
        guard.check();
        try {direct.write(b,off,len);}catch(IOException error){guard.check();throw error;}
        guard.check();
    }
    public void write(int b)throws IOException {
        guard.check();
        try {direct.write(b);}catch(IOException error){guard.check();throw error;}
        guard.check();
    }
    // LocalSocket.write already writes to the kernel. Its flush polls TIOCOUTQ until
    // the peer drains the send queue, which can stall the handshake. Do not poll it.
    // Buffered protocol wrappers above this stream still flush their own buffers.
    public void flush()throws IOException {guard.check();}
    public void close()throws IOException {guard.close();}
}
