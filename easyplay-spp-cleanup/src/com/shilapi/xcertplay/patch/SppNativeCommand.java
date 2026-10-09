package com.shilapi.xcertplay.patch;
import java.nio.charset.Charset;
/** Exact verified gocsdk profile only: no index invokes its managed-SPP release loop. */
public final class SppNativeCommand {
    private SppNativeCommand() {}
    public static byte[] releaseAll() { return "AT#VH\r\n".getBytes(Charset.forName("US-ASCII")); }
}
