package com.shilapi.xcertplay.patch;
import java.io.*;
import java.security.MessageDigest;
import org.junit.Test;
import static org.junit.Assert.*;
public class Bc03ProfilesTest {
    private String hash(File f)throws Exception {
        assertTrue("Missing supplied firmware "+f,f.isFile());
        MessageDigest digest=MessageDigest.getInstance("SHA-256");InputStream in=new FileInputStream(f);
        try{byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}finally{in.close();}
        StringBuilder s=new StringBuilder();for(byte v:digest.digest())s.append(String.format(java.util.Locale.US,"%02x",v&255));return s.toString();
    }
    private void rejected(String version,String apk,String daemon)throws Exception {
        try{Bc03Profiles.verified(version,apk,daemon);fail("Unverified firmware accepted");}catch(IOException expected){}
    }
    @Test public void threeSuppliedFirmwaresRouteInOneBuild()throws Exception {
        String daemon=hash(new File(".private/vendor-analysis/bc03-172/gocsdk"));
        String[] versions={"1.7.2","1.7.9","1.3.6"},folders={"bc03-172","bc03-179","bc03"};
        for(int i=0;i<versions.length;i++){
            Bc03Profiles.Profile p=Bc03Profiles.verified(versions[i],hash(new File(".private/vendor-analysis/"+folders[i]+"/input.apk")),daemon);
            assertEquals(versions[i],p.version);assertEquals(i==0,p.probeWithoutStatus);
            Bc03Schema s=Bc03Schema.read(".private/vendor-analysis/"+folders[i]+"/input.apk");
            assertEquals(48,s.code("SppDisConnect"));assertEquals(50,s.code("isSppConnect"));assertTrue(s.peerLayoutVerified);
        }
    }
    @Test public void intermediateVersionsAreNotAssumedCompatible()throws Exception {rejected("1.7.5",Bc03Profiles.forVersion("1.7.9").apkSha,Bc03Profiles.DAEMON_SHA);}
    @Test public void sameLabelDifferentApkIsRejected()throws Exception {rejected("1.7.2","different",Bc03Profiles.DAEMON_SHA);}
    @Test public void wrongVersionHashPairIsRejected()throws Exception {rejected("1.7.9",Bc03Profiles.forVersion("1.7.2").apkSha,Bc03Profiles.DAEMON_SHA);}
    @Test public void changedDaemonIsRejected()throws Exception {rejected("1.7.2",Bc03Profiles.forVersion("1.7.2").apkSha,"different");}
    @Test public void nullVersionIsRejected()throws Exception {rejected(null,"","");}
    @Test public void decoratedVersionLabelIsNotTrusted()throws Exception {rejected("1.7.2-custom",Bc03Profiles.forVersion("1.7.2").apkSha,Bc03Profiles.DAEMON_SHA);}
    @Test public void known179NeverUsesMissingStatusProbe()throws Exception {assertFalse(Bc03Profiles.forVersion("1.7.9").probeWithoutStatus);assertFalse(Bc03Profiles.forVersion("1.3.6").probeWithoutStatus);}
}
