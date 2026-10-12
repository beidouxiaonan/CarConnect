package com.shilapi.xcertplay.patch;
import org.junit.Test;import static org.junit.Assert.*;
public class SppNativeCommandTest {
    @Test public void exactNoIndexFrameOnlyDisconnectsManagedSpp()throws Exception {
        assertArrayEquals(new byte[]{65,84,35,86,72,13,10},SppNativeCommand.releaseAll());
        String text=new String(SppNativeCommand.releaseAll(),"US-ASCII");
        assertEquals("AT#VH\r\n",text);assertFalse(text.contains("VF"));assertFalse(text.contains("MW"));
    }
    @Test public void callReturnsFreshFrameWithoutRetainingCallerChanges(){
        byte[] first=SppNativeCommand.releaseAll();first[0]=0;assertEquals(65,SppNativeCommand.releaseAll()[0]);
    }
}
