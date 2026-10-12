package com.shilapi.xcertplay.patch;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Non-exported provider; only explicitly granted diagnostic report URIs are readable. */
public final class OemDiagnosticProvider extends ContentProvider {
    public static final String AUTHORITY="com.shihab.diplay.legacy.carconnect.diagnostics";
    public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(!AUTHORITY.equals(uri.getAuthority())||uri.getPathSegments().size()!=1||name==null||!name.matches("CarConnect-OEM-[0-9]{13}\\.txt"))throw new FileNotFoundException("Invalid report URI");
        File file=new File(new File(getContext().getFilesDir(),"oem-shared"),name);
        if(!file.isFile())throw new FileNotFoundException("Report not found");return file;
    }
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try{File f=file(uri);MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{f.getName(),f.length()});return c;}
        catch(FileNotFoundException error){return null;}
    }
    public String getType(Uri uri){return "text/plain";}
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
    public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
}
