package com.picviewer.app;
import java.util.*;
/** Bounded decoded-frame cache. Taking a frame transfers ownership to the player. */
final class NextFrame {
 static final long MIB=1024L*1024;long budget=10*MIB;int count=1;long bytes=0;
 static final class Entry{final Images.Decoded frame;final Exception failure;Entry(Images.Decoded f,Exception e){frame=f;failure=e;}}
 private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(8,.75f,true);
 synchronized boolean has(String key){return entries.containsKey(key);}
 synchronized Images.Decoded take(String key)throws Exception{Entry e=entries.remove(key);if(e==null)return null;if(e.failure!=null)throw e.failure;bytes-=size(e);return e.frame;}
 synchronized void configure(long limit,int frames){budget=Math.max(0,Math.min(48*MIB,limit));count=Math.max(0,Math.min(6,frames));trim();}
 synchronized void put(String key,Images.Decoded decoded){Entry old=entries.remove(key);discard(old);long size=decoded.bitmap.getAllocationByteCount();if(size>10*MIB||size>budget||count==0){decoded.bitmap.recycle();return;}entries.put(key,new Entry(decoded,null));bytes+=size;trim();}
 synchronized boolean offer(String key,Images.Decoded decoded){long n=decoded.bitmap.getAllocationByteCount();if(n>10*MIB||n+bytes>budget||entries.size()>=count){decoded.bitmap.recycle();return false;}put(key,decoded);return true;}
 synchronized void prioritize(java.util.List<String> keys){java.util.LinkedHashMap<String,Entry> kept=new java.util.LinkedHashMap<>();long used=0;for(String key:keys){Entry e=entries.remove(key);if(e==null)continue;if(kept.size()<count&&used+size(e)<=budget){kept.put(key,e);used+=size(e);}else discard(e);}for(Entry e:entries.values())discard(e);entries.clear();java.util.List<String> order=new java.util.ArrayList<>(kept.keySet());java.util.Collections.reverse(order);for(String key:order)entries.put(key,kept.get(key));bytes=used;}
 synchronized void fail(String key,Exception error){discard(entries.remove(key));if(count>0){entries.put(key,new Entry(null,error));trim();}}
 synchronized void clear(){for(Entry e:entries.values())discard(e);entries.clear();bytes=0;}
 private long size(Entry e){return e==null||e.frame==null?0:e.frame.bitmap.getAllocationByteCount();}
 private void discard(Entry e){if(e!=null&&e.frame!=null){bytes-=size(e);if(!e.frame.bitmap.isRecycled())e.frame.bitmap.recycle();}}
 private void trim(){Iterator<Entry> it=entries.values().iterator();while(it.hasNext()&&(entries.size()>count||bytes>budget)){Entry e=it.next();it.remove();discard(e);}}
 static long budget(long heapMax,long heapUsed,long nativeUsed,long systemAvailable,long systemThreshold,boolean low){if(low)return 0;long spare=heapMax-heapUsed-nativeUsed-Math.max(64*MIB,heapMax/3);long system=systemAvailable-systemThreshold-64*MIB;return Math.max(0,Math.min(48*MIB,Math.min(heapMax/8,Math.min(spare/2,system/8))));}
}
