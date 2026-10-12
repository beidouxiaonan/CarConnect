package com.shilapi.xcertplay.patch;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

/** Wire format verified against the supplied BC03 1.7.9 and native gocsdk. */
public final class OemProtocol {
    static final String IAP2_UUID = "00000000DECAFADEDECADEAFDECACAFE";
    static final Charset ASCII = Charset.forName("US-ASCII");
    public static String normalizeMac(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.US);
        if (s.matches("[0-9A-F]{12}")) {
            StringBuilder out = new StringBuilder();
            for (int i=0;i<12;i+=2) { if(i>0)out.append(':');out.append(s.substring(i,i+2)); }
            s = out.toString();
        }
        if (!s.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}") || s.equals("00:00:00:00:00:00")
            || s.equals("FF:FF:FF:FF:FF:FF") || s.equals("02:00:00:00:00:00")) return null;
        return s;
    }
    static String compactMac(String raw) throws IOException {
        String mac = normalizeMac(raw);
        if(mac == null)throw new IOException("BC03 未上报有效蓝牙地址");
        return mac.replace(":", "");
    }
    static byte[] connectCommand(String raw) throws IOException {
        return ("AT#VF" + compactMac(raw) + IAP2_UUID + "\r\n").getBytes(ASCII);
    }
    static byte[] socketHello(String raw) throws IOException { return compactMac(raw).getBytes(ASCII); }
}
