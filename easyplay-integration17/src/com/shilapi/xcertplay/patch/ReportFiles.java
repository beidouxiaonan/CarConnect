package com.shilapi.xcertplay.patch;
import java.io.*;
import java.util.*;
/** Only our generated names in caller-selected, fixed directories; no share application required. */
final class ReportFiles {
    static final int COPY_LIMIT=60000;
    static String excerpt(String report){
        if(report.length()<=COPY_LIMIT)return report;
        return report.substring(0,12000)+"\n[复制内容过长，中段省略；完整日志请使用保存文件]\n"
            +report.substring(report.length()-(COPY_LIMIT-12200));
    }
    static File save(String text,String name,File... folders)throws IOException {
        if(!name.matches("CarConnect-OEM-[0-9]{13}\\.txt"))throw new IOException("Invalid report name");
        IOException failure=new IOException("没有可写的诊断目录");
        for(File dir:folders){
            if(dir==null)continue;
            File target=new File(dir,name),temp=new File(dir,name+".tmp");
            try{
                if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create report directory");
                FileOutputStream stream=new FileOutputStream(temp);
                try{Writer writer=new OutputStreamWriter(stream,"UTF-8");writer.write(text);writer.flush();stream.getFD().sync();}
                finally{stream.close();}
                if(!temp.renameTo(target))throw new IOException("Cannot finalize report");
                File[] files=dir.listFiles(new FilenameFilter(){public boolean accept(File d,String n){return n.matches("CarConnect-OEM-[0-9]{13}\\.txt");}});
                if(files!=null){Arrays.sort(files,new Comparator<File>(){public int compare(File a,File b){return a.getName().compareTo(b.getName());}});
                    for(int i=0;i<files.length-5;i++)if(!files[i].equals(target))files[i].delete();}
                return target;
            }catch(IOException error){failure=error;temp.delete();}
            catch(SecurityException denied){failure=new IOException("Storage permission denied",denied);}
        }
        throw failure;
    }
}
