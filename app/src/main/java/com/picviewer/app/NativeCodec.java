package com.picviewer.app;
import android.graphics.Bitmap;
final class NativeCodec {
 private static boolean loaded;static{try{System.loadLibrary("piccodec");loaded=true;}catch(UnsatisfiedLinkError e){loaded=false;}}
 static boolean available(){return loaded;}
 static synchronized Bitmap decode(String path,int maxEdge,int kind){return decodeNative(path,maxEdge,kind);}
 private static native Bitmap decodeNative(String path,int maxEdge,int kind);
}
