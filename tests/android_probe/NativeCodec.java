package com.picviewer.app;
import android.graphics.Bitmap;
public class NativeCodec {
 private static native Bitmap decodeNative(String path,int edge,int kind);
 public static void main(String[] args){try{System.load("/data/local/tmp/libpiccodec.so");System.out.println("JNI_LIBRARY_LOADED");for(int kind:new int[]{2,0,1}){long start=System.currentTimeMillis();Bitmap b=decodeNative(kind==2?"/data/local/tmp/picviewer-test.tif":kind==1?"/data/local/tmp/picviewer-test.dng":"/data/local/tmp/picviewer-test.NEF",1024,kind);if(b==null)throw new AssertionError("null kind="+kind);System.out.println("JNI_PASS kind="+kind+" size="+b.getWidth()+"x"+b.getHeight()+" first="+Integer.toHexString(b.getPixel(0,0))+" ms="+(System.currentTimeMillis()-start));if(kind==2&&b.getPixel(0,0)!=0xff645032)throw new AssertionError("TIFF pixels");b.recycle();}if(decodeNative("/data/local/tmp/picviewer-broken.tif",1024,2)!=null)throw new AssertionError("malformed accepted");System.out.println("JNI_ALL_PASS");}catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
