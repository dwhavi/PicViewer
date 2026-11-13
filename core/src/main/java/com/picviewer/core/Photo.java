package com.picviewer.core;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** No device/network dependencies. Paths are normalized share-relative paths. */
public final class Photo implements Serializable {
 public final String path; public final long size, modified;
 public Photo(String path,long size,long modified) { this.path=Paths.normalize(path);this.size=size;this.modified=modified; }
 public String folder(){return Paths.parent(path);}
 public String name(){int i=path.lastIndexOf('/');return path.substring(i+1);}
 public String extension(){int i=path.lastIndexOf('.');return i<0?"":path.substring(i+1).toLowerCase(Locale.ROOT);}
 public String key(String source){try{byte[] b=MessageDigest.getInstance("SHA-256").digest((source+"\n"+path+"\n"+size+"\n"+modified).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public boolean sameVersion(Photo p){return p!=null&&path.equals(p.path)&&size==p.size&&modified==p.modified;}
 public static final Set<String> EXTENSIONS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("jpg","jpeg","jpe","png","webp","gif","bmp","heic","heif","avif","tif","tiff","dng","crw","cr2","cr3","nef","nrw","arw","sr2","srf","orf","rw2","raw","raf","pef","srw","kdc","dcr","mos","mrw","3fr","fff","iiq","rwl","x3f","jxl","jp2","j2k","jxr","tga","pcx","ico","psd","psb","svg")));
 public static final Set<String> RAW=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("dng","crw","cr2","cr3","nef","nrw","arw","sr2","srf","orf","rw2","raw","raf","pef","srw","kdc","dcr","mos","mrw","3fr","fff","iiq","rwl","x3f")));
 public static boolean candidate(String path){int i=path.lastIndexOf('.');return i>=0&&EXTENSIONS.contains(path.substring(i+1).toLowerCase(Locale.ROOT));}
 @Override public boolean equals(Object o){return o instanceof Photo&&sameVersion((Photo)o);}
 @Override public int hashCode(){return Objects.hash(path,size,modified);}
}
