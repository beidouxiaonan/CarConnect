package com.shilapi.xcertplay.patch;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/** Required private inputs: these integration checks must never silently skip. */
public class Bc03Firmware172Test {
    private Bc03Schema supplied() throws Exception {
        File apk=new File(".private/vendor-analysis/bc03-172/input.apk");
        assertTrue("Provide the verified BC03 1.7.2 APK", apk.isFile());
        return Bc03Schema.read(apk.getPath());
    }
    @Test public void actual172TransactionsAndPeerPrefixAreSupported() throws Exception {
        Bc03Schema s=supplied();
        String[] getters={"isBlueToothPowerOn","isConnectDevice","isConnectHFP","isConnectA2DP",
            "getLocalDeviceName","getLocalDeviceAddress","getConnectDevice","isSppConnect"};
        int[] expected={3,6,35,36,10,20,38,50};
        for(int i=0;i<getters.length;i++)assertEquals(getters[i],expected[i],s.code(getters[i]));
        assertTrue("MAC, name are the first two parcel fields",s.peerLayoutVerified);
    }
    @Test public void actual172Matches179And136ReadOnlySchemas() throws Exception {
        Bc03Schema s=supplied();
        for(String folder:new String[]{"bc03-179","bc03"}) {
            File other=new File(".private/vendor-analysis/"+folder+"/input.apk");
            assertTrue("Missing regression input "+folder,other.isFile());
            Bc03Schema old=Bc03Schema.read(other.getPath());
            for(String name:new String[]{"isBlueToothPowerOn","isConnectDevice","isConnectHFP","isConnectA2DP",
                    "getLocalDeviceName","getLocalDeviceAddress","getConnectDevice","isSppConnect"})
                assertEquals(name,old.code(name),s.code(name));
            assertEquals(old.peerLayoutVerified,s.peerLayoutVerified);
        }
    }
    @Test public void emptySppConnectControlCannotBeUsedAsAReadOnlyGetter() throws Exception {
        try {supplied().code("SppConnect");fail("Unverified control transaction accepted");}
        catch(IOException expected) {assertTrue(expected.getMessage().contains("SppConnect"));}
    }
    @Test public void actual172DisconnectRemainsVerifiedVoidNoArgumentCall()throws Exception {
        assertEquals(48,supplied().code("SppDisConnect"));
    }
}
