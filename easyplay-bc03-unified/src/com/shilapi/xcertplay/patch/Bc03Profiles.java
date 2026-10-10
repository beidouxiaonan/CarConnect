package com.shilapi.xcertplay.patch;

import java.io.IOException;

/** Exact supplied firmware profiles, not a numeric version interval. */
final class Bc03Profiles {
    static final String DAEMON_SHA="3e8f5687b69623087145177358c2104aeae50f2eb943db8711af983e395ced60";
    private static final Profile[] PROFILES={
        new Profile("1.7.2","32b25ec4eacada6bad179e4015f1c9ae8f9ce4df4fee9ee8baf6e99a781ede58",true),
        new Profile("1.7.9","079e6446c475eaf7d067853cf525feedbf9a9619408db6df83d873c200616326",false),
        new Profile("1.3.6","40c9cd2efd64a573303cb8bd1cf9061ab0f8f43cf161bddf4fb483f617e69349",false)
    };
    static Profile forVersion(String version)throws IOException {
        for(Profile p:PROFILES)if(p.version.equals(version))return p;
        throw new IOException("未核对的 BC03 版本 "+version+"；统一包当前核对 1.7.2 / 1.7.9 / 1.3.6，未发送原车连接命令");
    }
    static Profile verified(String version,String apkSha,String daemonSha)throws IOException {
        Profile p=forVersion(version);
        if(!p.apkSha.equals(apkSha))throw new IOException("BC03 "+version+" APK 指纹不匹配，未发送连接命令；SHA256="+apkSha);
        if(!DAEMON_SHA.equals(daemonSha))throw new IOException("gocsdk 指纹不匹配，未发送连接命令；SHA256="+daemonSha);
        return p;
    }
    static final class Profile {
        final String version,apkSha;
        final boolean probeWithoutStatus;
        Profile(String version,String apkSha,boolean probe){this.version=version;this.apkSha=apkSha;probeWithoutStatus=probe;}
    }
}
