package com.shilapi.xcertplay.patch;
import java.io.*;
/** Explicit export path. Never falls back, overwrites an existing report, or deletes older exports. */
final class AllLogsFiles {
    static File folder(String text)throws IOException {
        if(text==null||text.trim().length()==0||text.length()>1024||text.indexOf('\0')>=0)throw new IOException("请输入完整文件夹路径");
        File f=new File(text.trim());if(!f.isAbsolute())throw new IOException("请使用以 / 开头的绝对文件夹路径");
        f=f.getCanonicalFile();String p=f.getPath();
        if(f.getParentFile()==null)throw new IOException("请选择具体文件夹，不使用存储根目录");
        for(String system:new String[]{"/dev","/proc","/sys","/system","/data"})
            if(p.equals(system)||p.startsWith(system+"/"))throw new IOException("请选择外部存储文件夹，不写入系统目录");
        return f;
    }
    static File save(String report,File folder,long timestamp)throws IOException {
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("无法创建指定文件夹："+folder);
        File temp=File.createTempFile("CarConnect-AllLogs-"+timestamp+"-",".tmp",folder);
        boolean saved=false;
        try{
            FileOutputStream stream=new FileOutputStream(temp);
            try{Writer writer=new OutputStreamWriter(stream,"UTF-8");writer.write(report);writer.flush();stream.getFD().sync();}
            finally{stream.close();}
            File target=new File(folder,temp.getName().substring(0,temp.getName().length()-4)+".txt");
            if(target.exists()||!temp.renameTo(target))throw new IOException("无法完成导出文件，请检查文件夹权限");
            saved=true;return target;
        }finally{if(!saved)temp.delete();}
    }
    static String redact(String text){
        if(text==null)return "暂无记录";
        return text.replaceAll("(?i)(?<![0-9a-f])(?:[0-9a-f]{2}:){5}[0-9a-f]{2}(?![0-9a-f])","[MAC]")
            .replaceAll("(?i)(?<![0-9a-f])[0-9a-f]{12}(?![0-9a-f])","[MAC]")
            .replaceAll("(?i)((?:password|passphrase|privateKey|secret|accessToken)\\s*[=:]\\s*)[^\\r\\n;]+","$1[REDACTED]");
    }
    static String readKnown(File files,String name)throws IOException {
        if(!"legacy-session-status.txt".equals(name)&&!"legacy-device-diagnostics.txt".equals(name))throw new IOException("Unknown report source");
        File source=new File(files,name);
        if(!source.isFile())return "暂无保存记录："+name;
        if(!source.getCanonicalFile().getParentFile().equals(files.getCanonicalFile()))throw new IOException("诊断文件路径超出本应用目录");
        Reader reader=new InputStreamReader(new FileInputStream(source),"UTF-8");StringBuilder out=new StringBuilder();
        try{
            char[] buffer=new char[4096];int n,limit=1024*1024;
            while((n=reader.read(buffer))!=-1){int take=Math.min(n,limit-out.length());out.append(buffer,0,take);
                if(take<n||out.length()==limit){out.append("\n[该原有记录超过 1Mi 字符，后续未读；其他章节不受影响]\n");break;}}
        }finally{reader.close();}
        return redact(out.toString());
    }
}
