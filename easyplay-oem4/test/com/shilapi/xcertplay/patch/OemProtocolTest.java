package com.shilapi.xcertplay.patch;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class OemProtocolTest {
    @Test public void unpunctuatedOemMacIsNormalized(){assertEquals("A1:B2:C3:D4:E5:F6",OemProtocol.normalizeMac("a1b2c3d4e5f6"));}
    @Test public void lowercaseStandardMacIsNormalized(){assertEquals("A1:B2:C3:D4:E5:F6",OemProtocol.normalizeMac(" a1:b2:c3:d4:e5:f6 "));}
    @Test public void invalidAndPlaceholderMacsRejected(){
        for(String s:new String[]{null,"","BDUU","00112233445","00112233445566","0011223344ZZ","00:00:00:00:00:00","FF:FF:FF:FF:FF:FF","02:00:00:00:00:00","A1:B2:C3:D4:E5:F6\r\nAT#VH"})assertNull(OemProtocol.normalizeMac(s));
    }
    @Test public void iap2RequestContainsExact32HexUuid()throws Exception{
        assertEquals("AT#VFA1B2C3D4E5F600000000DECAFADEDECADEAFDECACAFE\r\n",new String(OemProtocol.connectCommand("A1:B2:C3:D4:E5:F6"),OemProtocol.ASCII));
        assertEquals(51,OemProtocol.connectCommand("A1:B2:C3:D4:E5:F6").length);
    }
    @Test public void socketGreetingIsOnlyTwelveAddressBytes()throws Exception{
        assertEquals("A1B2C3D4E5F6",new String(OemProtocol.socketHello("a1:b2:c3:d4:e5:f6"),OemProtocol.ASCII));
        assertEquals(12,OemProtocol.socketHello("A1B2C3D4E5F6").length);
    }
    @Test(expected=IOException.class) public void invalidTargetCannotReachWire()throws Exception{OemProtocol.connectCommand("T3");}
    @Test public void supplied179SchemaExposesOnlyVerifiedGetters()throws Exception{
        File f=new File(".private/vendor-analysis/bc03-179/input.apk");org.junit.Assume.assumeTrue(f.isFile());
        Bc03Schema s=Bc03Schema.read(f.getPath());assertEquals(20,s.code("getLocalDeviceAddress"));assertEquals(50,s.code("isSppConnect"));
        assertEquals(38,s.code("getConnectDevice"));assertTrue(s.peerLayoutVerified);
        try{s.code("SppConnect");fail();}catch(IOException expected){}
    }
}
