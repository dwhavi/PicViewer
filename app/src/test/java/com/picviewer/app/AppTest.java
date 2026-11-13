package com.picviewer.app;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;import com.picviewer.core.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h600dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class AppTest {
 MainActivity a;
 @Before public void init(){a=Robolectric.buildActivity(MainActivity.class).setup().get();}
 @After public void done(){if(a!=null)a.onDestroy();}
 @Test public void welcomeRenders(){assertNotNull(a.root);capture("welcome");}
 @Test public void demoSelectionAndGallery(){a.startDemo();assertEquals(4,a.all.size());assertEquals(4,a.displayed.size());a.selection.set("여행",false);a.refreshSelection();assertEquals(2,a.displayed.size());a.selection.all("",true);a.refreshSelection();capture("gallery");}
 @Test public void emptySelectionIsSafe(){a.startDemo();a.selection.all("",false);a.refreshSelection();assertEquals(0,a.displayed.size());assertFalse(a.bottom.getChildAt(a.bottom.getChildCount()-1).isEnabled());}
 @Test public void pcOffStateRenders(){a.showStatus("PC에 연결할 수 없습니다","OFFLINE");capture("pc-off");}
 @Test public void authStateRenders(){a.showStatus("계정 또는 폴더를 확인해 주세요","AUTH");capture("auth");}
 @Test public void databaseRoundTrip(){IndexDb d=new IndexDb(a);java.util.List<Photo> p=java.util.Arrays.asList(new Photo("a/사진2.jpg",52,99));java.util.Set<String> f=new java.util.HashSet<>(java.util.Arrays.asList("","a"));d.replace(p,f);assertEquals(p,d.photos());assertEquals(f,d.folders());d.remove(p.get(0).path);assertTrue(d.photos().isEmpty());d.clear();d.close();}
 @Test public void sampleDimensionBounded(){assertEquals(4,Images.sample(8000,6000,1000,750));assertEquals(1,Images.sample(400,300,1000,750));}
 @Test public void platformImageDecode()throws Exception{a.startDemo();for(Photo p:a.all){Images.Decoded d=a.images.decode(p,960,600);assertNotNull(p.path,d.bitmap);assertTrue(d.bitmap.getWidth()<=1920);d.bitmap.recycle();}}
 @Test public void cacheKeepsOnlyLiveVersions()throws Exception{a.startDemo();a.images.file(a.all.get(0));assertEquals(1,a.images.cached(a.all).size());a.images.reconcile(java.util.Collections.emptyList());assertEquals(0,a.images.cached(a.all).size());}
 void capture(String name){try{View v=a.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));v.layout(0,0,960,600);a.thumbs.submit(()->{}).get(15,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();v.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));v.layout(0,0,960,600);Bitmap b=Bitmap.createBitmap(960,600,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));File out=new File(System.getProperty("picviewer.screenshots","build/screenshots"));out.mkdirs();try(OutputStream s=new FileOutputStream(new File(out,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,s);}b.recycle();}catch(Exception e){throw new AssertionError(e);}}
}
