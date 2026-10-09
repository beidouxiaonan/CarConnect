package com.shilapi.xcertplay.patch;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class Bc03SchemaTest {
    private static final String[] N={"isBlueToothPowerOn","isConnectDevice","isConnectHFP","isConnectA2DP","getLocalDeviceName"};
    private Map<String,String> signatures() {
        Map<String,String> s=new HashMap<String,String>();
        for(String n:N)s.put(n,n.equals("getLocalDeviceName")?"()Ljava/lang/String;":"()Z");return s;
    }
    private Map<String,Object> codes() {
        Map<String,Object> c=new HashMap<String,Object>();c.put("DESCRIPTOR","com.bt.BTFeature");
        for(int i=0;i<N.length;i++)c.put("TRANSACTION_"+N[i],i+70);return c;
    }
    @Test public void suppliedOemApkMetadataMatchesVerifiedSchema() throws Exception {
        org.junit.Assume.assumeTrue(new File(".private/vendor-analysis/bc03/input.apk").isFile());
        Bc03Schema s=Bc03Schema.read(".private/vendor-analysis/bc03/input.apk");
        assertEquals(3,s.code(N[0]));assertEquals(10,s.code(N[4]));assertEquals(38,s.code("getConnectDevice"));assertTrue(s.peerLayoutVerified);
    }
    @Test public void bc03FirmwareVersions136And179ExposeIdenticalSchema() throws Exception {
        File f136=new File(".private/vendor-analysis/bc03/input.apk");
        File f179=new File(".private/vendor-analysis/bc03-179/input.apk");
        org.junit.Assume.assumeTrue(f136.isFile()&&f179.isFile());
        Bc03Schema s136=Bc03Schema.read(f136.getPath());
        Bc03Schema s179=Bc03Schema.read(f179.getPath());
        String[] all={"isBlueToothPowerOn","isConnectDevice","isConnectHFP","isConnectA2DP","getLocalDeviceName",
            "getLocalDeviceAddress","isSppConnect","getConnectDevice"};
        for(String n:all)assertEquals(n,s179.code(n),s136.code(n));
        assertTrue(s136.peerLayoutVerified);
        assertEquals(s179.peerLayoutVerified,s136.peerLayoutVerified);
    }
    @Test public void changedTransactionNumbersUseActualMap() throws Exception {
        Bc03Schema s=Bc03Schema.validate(signatures(),codes(),false);
        assertEquals(70,s.code(N[0]));assertEquals(74,s.code(N[4]));assertFalse(s.peerLayoutVerified);
    }
    @Test(expected=IOException.class) public void incompatibleSignatureRejected() throws Exception {
        Map<String,String> s=signatures();s.put(N[0],"(I)Z");Bc03Schema.validate(s,codes(),false);
    }
    @Test(expected=IOException.class) public void duplicateTransactionRejected() throws Exception {
        Map<String,Object> c=codes();c.put("TRANSACTION_"+N[1],70);Bc03Schema.validate(signatures(),c,false);
    }
    @Test(expected=IOException.class) public void missingConstantRejected() throws Exception {
        Map<String,Object> c=codes();c.remove("TRANSACTION_"+N[0]);Bc03Schema.validate(signatures(),c,false);
    }
    @Test(expected=IOException.class) public void wrongDescriptorRejected() throws Exception {
        Map<String,Object> c=codes();c.put("DESCRIPTOR","another.Interface");Bc03Schema.validate(signatures(),c,false);
    }
    @Test(expected=IOException.class) public void unverifiedParcelableCannotBeQueried() throws Exception {
        Bc03Schema.validate(signatures(),codes(),false).code("getConnectDevice");
    }
    @Test public void truncatedDexFailsCleanly() throws Exception {
        File f=File.createTempFile("easyplay-schema-",".apk");
        try {
            ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f));
            z.putNextEntry(new ZipEntry("classes.dex"));z.write(new byte[]{1,2,3});z.close();
            try {Bc03Schema.read(f.getAbsolutePath());fail();}catch(IOException expected){}
        } finally {assertTrue(f.delete());}
    }
    @Test public void verifiedDisconnectSignatureAndActualTransactions()throws Exception {
        for(String version:new String[]{"bc03","bc03-179"}) {
            File f=new File(".private/vendor-analysis/"+version+"/input.apk");org.junit.Assume.assumeTrue(f.isFile());
            assertEquals(48,Bc03Schema.read(f.getPath()).code("SppDisConnect"));
        }
    }
    @Test(expected=IOException.class) public void wrongDisconnectSignatureRejected()throws Exception {
        Map<String,String>s=signatures();Map<String,Object>c=codes();s.put("SppDisConnect","(I)V");c.put("TRANSACTION_SppDisConnect",48);
        Bc03Schema.validate(s,c,false);
    }
    @Test(expected=IOException.class) public void duplicateDisconnectCodeRejected()throws Exception {
        Map<String,String>s=signatures();Map<String,Object>c=codes();s.put("SppDisConnect","()V");c.put("TRANSACTION_SppDisConnect",70);
        Bc03Schema.validate(s,c,false);
    }
    @Test(expected=IOException.class) public void absentDisconnectCannotBeCalled()throws Exception {Bc03Schema.validate(signatures(),codes(),false).code("SppDisConnect");}
}
