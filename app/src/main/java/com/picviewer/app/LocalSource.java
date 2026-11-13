package com.picviewer.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import com.picviewer.core.*;
import java.io.*;
import java.util.*;

/** Read-only Storage Access Framework tree. No filesystem paths or broad storage permission. */
final class LocalSource implements PhotoSource {
 static final String[] COLUMNS={"document_id","_display_name","mime_type","_size","last_modified"};
 final ContentResolver resolver;final Uri tree;final Map<String,Uri> documents=new HashMap<>();
 LocalSource(Context context,String uri){resolver=context.getContentResolver();tree=Uri.parse(uri);documents.put("",DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)));}
 static final class Unavailable extends IOException {Unavailable(Throwable e){super("LOCAL_ACCESS",e);}Unavailable(String why){super(why);}}
 Uri resolve(String path)throws Exception{
  path=Paths.normalize(path);if(Paths.depth(path)>128)throw new Unavailable("LOCAL_DEPTH_LIMIT");Uri uri=documents.get(path);if(uri!=null)return uri;
  String parent=Paths.parent(path);list(parent);uri=documents.get(path);if(uri==null)throw new FileNotFoundException("LOCAL_MISSING");return uri;
 }
 public List<com.picviewer.core.Scanner.Entry> list(String path)throws Exception{
  try{Uri parent=resolve(path);String id=DocumentsContract.getDocumentId(parent);String ancestor=path;while(!ancestor.isEmpty()){ancestor=Paths.parent(ancestor);Uri previous=documents.get(ancestor);if(previous!=null&&id.equals(DocumentsContract.getDocumentId(previous)))throw new Unavailable("LOCAL_DIRECTORY_CYCLE");}Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
   List<com.picviewer.core.Scanner.Entry> result=new ArrayList<>();Set<String> names=new HashSet<>();
   try(Cursor cursor=resolver.query(children,COLUMNS,null,null,null)){
    if(cursor==null)throw new Unavailable("LOCAL_QUERY_FAILED");
    // Providers may return a partial loading result. Never treat that as confirmed deletion.
    if(cursor.getExtras().containsKey(DocumentsContract.EXTRA_ERROR))throw new Unavailable("LOCAL_PROVIDER_ERROR");
    if(cursor.getExtras().getBoolean(DocumentsContract.EXTRA_LOADING,false))throw new Unavailable("LOCAL_LOADING");
    while(cursor.moveToNext()){
     String doc=cursor.getString(0),name=cursor.getString(1),mime=cursor.getString(2);
     if(doc==null||name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0||name.indexOf('\\')>=0||name.indexOf(':')>=0||name.indexOf('\0')>=0)continue;
     if(!names.add(name))throw new Unavailable("LOCAL_DUPLICATE_NAME");
     String child=Paths.join(path,name);if(!child.equals(path.isEmpty()?name:path+"/"+name))throw new Unavailable("LOCAL_UNSUPPORTED_NAME");Uri childUri=DocumentsContract.buildDocumentUriUsingTree(tree,doc);documents.put(child,childUri);
     long size=cursor.isNull(3)?-1:cursor.getLong(3),modified=cursor.isNull(4)?-1:cursor.getLong(4);if(size<0||modified<=0)modified=-System.nanoTime();
     result.add(new com.picviewer.core.Scanner.Entry(name,DocumentsContract.Document.MIME_TYPE_DIR.equals(mime),false,size,modified));
    }
   }
   return result;
  }catch(SecurityException e){throw new com.picviewer.core.Scanner.Unavailable(new Unavailable(e));}
 }
 long[] version(Uri uri)throws Exception{try(Cursor c=resolver.query(uri,new String[]{"_size","last_modified"},null,null,null)){if(c==null||!c.moveToFirst())throw new FileNotFoundException("LOCAL_MISSING");return new long[]{c.isNull(0)?-1:c.getLong(0),c.isNull(1)?-1:c.getLong(1)};}}
 public void fetch(String path,File target,long max,long knownSize,long knownModified)throws Exception{
  try{documents.clear();documents.put("",DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)));Uri uri=resolve(path);long[] before=version(uri);if((knownSize>=0&&before[0]>=0&&knownSize!=before[0])||(knownModified>=0&&before[1]>=0&&knownModified!=before[1]))throw new IOException("FILE_CHANGED_SINCE_SCAN");if(before[0]>max)throw new IOException("FILE_LIMIT");
   try(InputStream in=resolver.openInputStream(uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(target))){if(in==null)throw new FileNotFoundException("LOCAL_MISSING");byte[] b=new byte[128*1024];long count=0;int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();count+=n;if(count>max)throw new IOException("FILE_LIMIT");out.write(b,0,n);}if(before[0]>=0&&count!=before[0])throw new IOException("FILE_CHANGED_DURING_READ");}
   long[] after=version(uri);if(!Arrays.equals(before,after))throw new IOException("FILE_CHANGED_DURING_READ");
  }catch(SecurityException e){throw new Unavailable(e);}
 }
 public void close(){documents.clear();}
}
