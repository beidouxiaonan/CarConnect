package com.shilapi.xcertplay.patch;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
/** Bounded diagnostics. Event producers never perform disk I/O. */
public final class OemTraceJournal {
    private final File dir; private final Object disk=new Object();
    private final ArrayDeque<String> recent=new ArrayDeque<String>();
    private final ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<String>(128);
    private int dropped; private volatile String diskError;
    private final Thread writer;
    static String clean(String text){
        if(text==null)return "null";
        text=text.replaceAll("(?i)(?<![0-9a-f])(?:[0-9a-f]{2}:){5}[0-9a-f]{2}(?![0-9a-f])","[MAC]")
            .replaceAll("(?i)(?<![0-9a-f])[0-9a-f]{12}(?![0-9a-f])","[MAC]").replace('\r',' ');
        return text.length()>2048?text.substring(0,2048)+" [truncated]":text;
    }
    public OemTraceJournal(File directory){
        dir=directory;
        writer=new Thread(new Runnable(){public void run(){
            try{
                while(true){String line=queue.poll(1,TimeUnit.SECONDS);if(line!=null)append(line);}
            }catch(InterruptedException stop){Thread.currentThread().interrupt();}
        }},"carconnect-oem-trace-writer");writer.setDaemon(true);writer.start();
    }
    public synchronized void event(String text){
        String line=clean(text);if(recent.size()==256)recent.removeFirst();recent.addLast(line);
        if(!queue.offer(line))dropped++;
    }
    private void append(String line){
        synchronized(disk){
            try{
                if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create trace directory");
                File active=new File(dir,"current.txt");
                if(active.length()>256*1024){
                    File oldest=new File(dir,"previous2.txt"),previous=new File(dir,"previous1.txt");
                    if(oldest.exists()&&!oldest.delete())throw new IOException("Trace rotation failed");
                    if(previous.exists()&&!previous.renameTo(oldest))throw new IOException("Trace rotation failed");
                    if(!active.renameTo(previous))throw new IOException("Trace rotation failed");
                }
                Writer out=new OutputStreamWriter(new FileOutputStream(active,true),"UTF-8");
                try{out.write(line);out.write('\n');}finally{out.close();}
            }catch(IOException error){diskError=clean(error.toString());}
        }
    }
    public String report()throws IOException {
        StringBuilder out=new StringBuilder();
        synchronized(disk){for(String name:new String[]{"previous2.txt","previous1.txt","current.txt"}){
            File file=new File(dir,name);if(!file.isFile())continue;
            out.append("\n--- ").append(name).append(" ---\n");
            Reader reader=new InputStreamReader(new FileInputStream(file),"UTF-8");
            try{char[] b=new char[4096];int n,total=0;while((n=reader.read(b))!=-1&&total<270000){out.append(b,0,n);total+=n;}}
            finally{reader.close();}
        }}
        synchronized(this){out.append("\n--- recent memory (may repeat persisted entries) ---\n");
            for(String line:recent)out.append(line).append('\n');
            out.append("disk queue dropped=").append(dropped).append("; disk error=").append(diskError).append('\n');}
        return out.toString();
    }
    // Host verification only; production lifetime is the application process.
    void stopWriter(){writer.interrupt();}
}
