package com.picviewer.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;
import com.picviewer.core.*;import java.util.*;
final class IndexDb extends SQLiteOpenHelper {
 IndexDb(Context c){super(c,"photos.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE photos(path TEXT PRIMARY KEY,size INTEGER,modified INTEGER)");d.execSQL("CREATE TABLE folders(path TEXT PRIMARY KEY)");}
 public void onUpgrade(SQLiteDatabase d,int old,int now){}
 synchronized List<Photo> photos(){List<Photo> a=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT path,size,modified FROM photos",null)){while(c.moveToNext())a.add(new Photo(c.getString(0),c.getLong(1),c.getLong(2)));}return a;}
 synchronized Set<String> folders(){Set<String> a=new TreeSet<>(Paths::natural);try(Cursor c=getReadableDatabase().rawQuery("SELECT path FROM folders",null)){while(c.moveToNext())a.add(c.getString(0));}return a;}
 synchronized void replace(List<Photo> photos,Set<String> folders){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{d.delete("photos",null,null);d.delete("folders",null,null);for(Photo p:photos){ContentValues v=new ContentValues();v.put("path",p.path);v.put("size",p.size);v.put("modified",p.modified);d.insertOrThrow("photos",null,v);}for(String p:folders){ContentValues v=new ContentValues();v.put("path",p);d.insertOrThrow("folders",null,v);}d.setTransactionSuccessful();}finally{d.endTransaction();}}
 synchronized void remove(String path){getWritableDatabase().delete("photos","path=?",new String[]{path});}
 synchronized void clear(){replace(new ArrayList<>(),new HashSet<>());}
}
