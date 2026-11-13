package com.picviewer.app;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import java.io.File;
@RunWith(AndroidJUnit4.class)
public class NativeDeviceTest {
 @Test public void libraryLoads(){assertTrue("native library must load on this ABI",NativeCodec.available());}
 @Test public void tiffPixels(){File f=new File("/data/local/tmp/picviewer-test.tif");assertTrue("push generated TIFF fixture first",f.isFile());Bitmap b=NativeCodec.decode(f.getAbsolutePath(),1024,2);assertNotNull(b);assertEquals(48,b.getWidth());assertEquals(32,b.getHeight());assertEquals(0xff645032,b.getPixel(0,0));b.recycle();}
 @Test public void rawPreviewAndBasicRender(){File f=new File("/data/local/tmp/picviewer-test.NEF");assertTrue("push CC0 Nikon D7000 fixture first",f.isFile());for(int kind:new int[]{0,1}){File sample=kind==1?new File("/data/local/tmp/picviewer-test.dng"):f;assertTrue(sample.isFile());Bitmap b=NativeCodec.decode(sample.getAbsolutePath(),1024,kind);assertNotNull("RAW kind "+kind,b);assertTrue(b.getWidth()<=1024);assertTrue(b.getHeight()<=1024);assertTrue(b.getWidth()>100);b.recycle();}}
 @Test public void brokenInputRejected(){File f=new File("/data/local/tmp/picviewer-broken.tif");assertTrue(f.isFile());assertNull(NativeCodec.decode(f.getAbsolutePath(),1024,2));}
}
