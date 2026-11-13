package com.picviewer.app;
import android.content.*;import android.graphics.*;
import com.picviewer.core.Photo;
import java.io.*;import java.util.*;

final class Images implements AutoCloseable {
 static final long MAX_FILE=256L*1024*1024; final Context context;final java.io.File dir;final Config config;SmbSource source;
 long maxCache=256L*1024*1024;volatile boolean online=true;boolean demo=false;volatile boolean closed=false;volatile long cacheEpoch=0;
 Images(Context c,Config config){context=c;this.config=config;dir=new java.io.File(c.getCacheDir(),"frames");dir.mkdirs();}
 synchronized java.io.File file(Photo p)throws Exception{
  if(closed)throw new IOException("CLOSED");long epoch=cacheEpoch;String key=p.key(config.source());java.io.File f=new java.io.File(dir,key+".img");if(f.exists()&&f.length()>0){f.setLastModified(System.currentTimeMillis());return f;}if(!online&&!demo)throw new IOException("OFFLINE_CACHE_MISS");if(p.size>MAX_FILE)throw new IOException("FILE_LIMIT");
  java.io.File tmp=new java.io.File(dir,key+".part");try{
   if(demo){try(InputStream in=context.getAssets().open("demo/"+p.name());OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}
   else {if(source==null)source=new SmbSource(config);source.fetch(p.path,tmp,MAX_FILE,p.size,p.modified);}
   if(closed||epoch!=cacheEpoch)throw new IOException("CANCELLED");if(tmp.length()==0)throw new IOException("EMPTY_FILE");if(!tmp.renameTo(f))throw new IOException("CACHE_WRITE");trim(f);return f;
  }catch(Exception e){tmp.delete();if(source!=null){source.close();source=null;}throw e;}
 }
 List<Photo> cached(List<Photo> photos){List<Photo> out=new ArrayList<>();for(Photo p:photos){java.io.File f=new java.io.File(dir,p.key(config.source())+".img");if(f.isFile()&&f.length()>0)out.add(p);}return out;}
 void reconcile(List<Photo> all){Set<String> keys=new HashSet<>();for(Photo p:all)keys.add(p.key(config.source())+".img");java.io.File[] files=dir.listFiles();if(files!=null)for(java.io.File f:files)if(!f.getName().endsWith(".part")&&!keys.contains(f.getName()))f.delete();}
 long bytes(){long n=0;java.io.File[] fs=dir.listFiles();if(fs!=null)for(java.io.File f:fs)n+=f.length();return n;}
 void clear(){cacheEpoch++;java.io.File[] fs=dir.listFiles();if(fs!=null)for(java.io.File f:fs)f.delete();}
 void trim(java.io.File keep){java.io.File[] fs=dir.listFiles();if(fs==null)return;Arrays.sort(fs,Comparator.comparingLong(java.io.File::lastModified));long bytes=bytes();for(java.io.File f:fs)if(bytes>maxCache&&!f.equals(keep)){bytes-=f.length();f.delete();}}
 static final class Decoded {final Bitmap bitmap;final String note;Decoded(Bitmap b,String n){bitmap=b;note=n;}}
 Decoded decode(Photo p,int width,int height)throws Exception{return decode(p,width,height,true);}
 Decoded decode(Photo p,int width,int height,boolean allowRender)throws Exception{
  java.io.File f=file(p);Bitmap bitmap=null;String note="";boolean oriented=false;boolean raw=Photo.RAW.contains(p.extension());
  if(raw){try{androidx.exifinterface.media.ExifInterface ex=new androidx.exifinterface.media.ExifInterface(f);byte[] preview=ex.getThumbnailBytes();if(preview!=null){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(preview,0,preview.length,o);o.inSampleSize=sample(o.outWidth,o.outHeight,width,height);o.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeByteArray(preview,0,preview.length,o);if(bitmap!=null)note="RAW 미리보기";}}catch(Exception ignored){}
   if(bitmap==null&&NativeCodec.available()){bitmap=NativeCodec.decode(f.getAbsolutePath(),Math.max(width,height),0);note="RAW 미리보기";if(bitmap==null&&allowRender){bitmap=NativeCodec.decode(f.getAbsolutePath(),Math.max(width,height),1);note="RAW 간이 현상";}}
  }
  if(bitmap==null){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getAbsolutePath(),o);if(o.outWidth>0&&o.outHeight>0){o.inSampleSize=sample(o.outWidth,o.outHeight,width,height);o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;bitmap=BitmapFactory.decodeFile(f.getAbsolutePath(),o);}
   if(bitmap==null&&android.os.Build.VERSION.SDK_INT>=28){try{ImageDecoder.Source src=ImageDecoder.createSource(f);bitmap=ImageDecoder.decodeBitmap(src,(decoder,info,source)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double s=Math.min(1.0,Math.min((double)width/w,(double)height/h));decoder.setTargetSize(Math.max(1,(int)(w*s)),Math.max(1,(int)(h*s)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});oriented=bitmap!=null;}catch(Exception ignored){}}
   if(bitmap==null&&!raw&&(p.extension().equals("tiff")||p.extension().equals("tif"))&&NativeCodec.available()){bitmap=NativeCodec.decode(f.getAbsolutePath(),Math.max(width,height),2);note="TIFF 확장 표시";oriented=bitmap!=null;}
  }
  if(bitmap==null)throw new IOException(raw?"RAW_PREVIEW_UNSUPPORTED":"FORMAT_UNSUPPORTED");
  if(!oriented&&!note.equals("RAW 간이 현상"))try{androidx.exifinterface.media.ExifInterface ex=new androidx.exifinterface.media.ExifInterface(f);int rotation=ex.getRotationDegrees();boolean flip=ex.isFlipped();if(rotation!=0||flip){Matrix m=new Matrix();if(flip)m.postScale(-1,1);m.postRotate(rotation);Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}}catch(Exception ignored){}
  return new Decoded(bitmap,note);
 }
 static int sample(int w,int h,int tw,int th){int n=1;while(w/n>Math.max(1,tw)*2||h/n>Math.max(1,th)*2)n*=2;return n;}
 public void close(){closed=true;Thread cleanup=new Thread(()->{synchronized(this){if(source!=null)source.close();source=null;}},"photo-source-close");cleanup.setDaemon(true);cleanup.start();}
}
