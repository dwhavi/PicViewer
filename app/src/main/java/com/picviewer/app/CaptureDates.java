package com.picviewer.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;import com.picviewer.core.*;import java.io.File;import java.util.*;import java.util.concurrent.*;
/** Read-only EXIF metadata, versioned by source/path/size/mtime. Never writes original photos. */
final class CaptureDates extends SQLiteOpenHelper {
 boolean closed=false;final Map<String,String> dates=new ConcurrentHashMap<>();
 CaptureDates(Context c){super(c,"capture-dates.db",null,1);try(Cursor r=getReadableDatabase().rawQuery("SELECT k,v FROM dates",null)){while(r.moveToNext())dates.put(r.getString(0),r.getString(1));}}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE dates(k TEXT PRIMARY KEY,v TEXT NOT NULL)");}public void onUpgrade(SQLiteDatabase d,int o,int n){}
 static String normalize(String value){if(value==null)return "";try{java.time.LocalDateTime d=java.time.LocalDateTime.parse(value.trim(),java.time.format.DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT));return d.format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));}catch(Exception e){return "";}}
 synchronized void read(Photo photo,String source,File f){if(closed)return;String key=photo.key(source);if(dates.containsKey(key))return;String value="";try{androidx.exifinterface.media.ExifInterface ex=new androidx.exifinterface.media.ExifInterface(f);value=normalize(ex.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL));}catch(Exception ignored){}ContentValues row=new ContentValues();row.put("k",key);row.put("v",value);getWritableDatabase().insertWithOnConflict("dates",null,row,SQLiteDatabase.CONFLICT_REPLACE);dates.put(key,value);}
 @Override public synchronized void close(){closed=true;super.close();}
 String date(Photo p,String source){return dates.getOrDefault(p.key(source),"");}
 Comparator<Photo> comparator(String source){return (a,b)->{String x=date(a,source),y=date(b,source);int c=x.isEmpty()?(y.isEmpty()?0:1):y.isEmpty()?-1:x.compareTo(y);if(c!=0)return c;c=Paths.natural(a.name(),b.name());return c!=0?c:Paths.natural(a.path,b.path);};}
}
